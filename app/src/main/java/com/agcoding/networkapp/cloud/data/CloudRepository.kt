package com.agcoding.networkapp.cloud.data

import com.agcoding.networkapp.cloud.data.local.SyncLinkDao
import com.agcoding.networkapp.cloud.data.network.NetworkMonitor
import com.agcoding.networkapp.cloud.data.remote.SupabaseClient
import com.agcoding.networkapp.cloud.data.remote.SupabaseException
import com.agcoding.networkapp.cloud.data.session.CloudSessionStore
import com.agcoding.networkapp.cloud.data.sync.SyncManager
import com.agcoding.networkapp.cloud.domain.model.CloudConnectionStatus
import com.agcoding.networkapp.cloud.domain.model.CloudError
import com.agcoding.networkapp.cloud.domain.model.CloudException
import com.agcoding.networkapp.cloud.domain.model.CloudSession
import com.agcoding.networkapp.cloud.domain.model.CloudState
import com.agcoding.networkapp.cloud.domain.model.HouseholdMember
import com.agcoding.networkapp.cloud.domain.model.SyncProgress
import com.agcoding.networkapp.cloud.domain.model.SyncStatus
import com.agcoding.networkapp.cloud.domain.model.SyncSummary
import com.agcoding.networkapp.cloud.domain.model.cloudConnectionStatus
import com.agcoding.networkapp.shared.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Account, household and sync actions for the Cloud screen. Failures come back as
 * [CloudException] with a [CloudError] the screen can explain.
 */
@Singleton
class CloudRepository @Inject constructor(
    private val client: SupabaseClient,
    private val store: CloudSessionStore,
    private val syncManager: SyncManager,
    private val linkDao: SyncLinkDao,
    private val networkMonitor: NetworkMonitor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    val state: Flow<CloudState> = store.state
    val syncStatus: StateFlow<SyncStatus> = syncManager.status
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val hasPendingChanges: StateFlow<Boolean> = syncManager.hasPendingChanges

    val connectionStatus: Flow<CloudConnectionStatus> =
        combine(store.state, syncManager.status, networkMonitor.isOnline, syncManager.hasPendingChanges) { state, status, online, pending ->
            cloudConnectionStatus(state, status, online, pending)
        }

    /** Remind to set up sync while the data lives only on this phone, unless snoozed. */
    val shouldPromptSync: Flow<Boolean> =
        combine(connectionStatus, store.promptSnoozedUntil) { status, snoozedUntil ->
            !status.isConnected && System.currentTimeMillis() >= snoozedUntil
        }

    suspend fun snoozeSyncPrompt(forMillis: Long) {
        store.snoozePrompt(System.currentTimeMillis() + forMillis)
    }

    /** Progress of the running sync, for the upload screen. */
    val syncProgress: StateFlow<SyncProgress?> = syncManager.progress

    /**
     * @param syncAfter sync right away if the user already has a household (the upload screen
     *   passes false and syncs itself to show progress)
     * @return whether the user already belongs to a household
     */
    suspend fun signIn(email: String, password: String, syncAfter: Boolean = true): Result<Boolean> = cloudCall {
        connect(signInOrRedeem(email.trim(), password), syncAfter)
    }

    /**
     * Signs in; when the password is wrong it may be the temporary one from "forgot password",
     * which then becomes the password (the session is marked to choose a new one).
     */
    private fun signInOrRedeem(email: String, password: String): CloudSession = try {
        client.signIn(email, password)
    } catch (e: SupabaseException) {
        val redeemed = e.code == "invalid_credentials" &&
            runCatching { client.redeemTempPassword(email, password) }.getOrDefault(false)
        if (!redeemed) throw e
        client.signIn(email, password)
    }

    /** "Forgot password": emails a temporary password to sign in with. */
    suspend fun requestTempPassword(email: String, language: String): Result<Unit> = cloudCall {
        client.requestTempPassword(email.trim(), language)
    }

    /** Whether the signed-in user has to choose a new password (signed in with a temporary one). */
    val mustChangePassword: Flow<Boolean> = store.state.map { it.session?.mustChangePassword == true }

    /**
     * Sets a new password. [currentPassword] is checked first, unless the user just signed in
     * with a temporary password.
     */
    suspend fun changePassword(currentPassword: String?, newPassword: String): Result<Unit> = cloudCall {
        val session = if (currentPassword != null) {
            val email = store.current().session?.email ?: throw CloudException(CloudError.SESSION_EXPIRED)
            client.signIn(email, currentPassword).also { store.saveSession(it) }
        } else {
            requireSession()
        }
        client.changePassword(session, newPassword)
        store.saveSession(session.copy(mustChangePassword = false))
    }

    /**
     * Deletes the cloud account after checking [password]. Households only this user is in are
     * deleted with their data; the data on this phone stays and it stops syncing.
     */
    suspend fun deleteAccount(password: String): Result<Unit> = cloudCall {
        val email = store.current().session?.email ?: throw CloudException(CloudError.SESSION_EXPIRED)
        val session = client.signIn(email, password)
        client.deleteAccount(session)
        store.clearSession()
        store.saveLinksHouseholdId(null)
        store.savePendingChanges(false)
        linkDao.clear()
    }

    /** Creates the account (no email verification) and signs in; same result as [signIn]. */
    suspend fun signUp(email: String, password: String, displayName: String, syncAfter: Boolean = true): Result<Boolean> = cloudCall {
        val session = client.signUp(email.trim(), password, displayName.trim())
        connect(session, syncAfter)
    }

    /** Creates a household and (unless [syncAfter] is false) uploads this device's data to it. */
    suspend fun createHousehold(name: String, syncAfter: Boolean = true): Result<Unit> = cloudCall {
        val session = requireSession()
        val household = client.createHousehold(session, name.trim())
        syncManager.prepareFor(household.id)
        store.saveHousehold(household)
        if (syncAfter) syncManager.syncNow().getOrThrow()
    }

    /** Joins with an invite code; this device's data is then replaced with the household's. */
    suspend fun joinHousehold(code: String, syncAfter: Boolean = true): Result<Unit> = cloudCall {
        val session = requireSession()
        val household = client.joinHousehold(session, code.trim())
        syncManager.prepareFor(household.id)
        store.saveHousehold(household)
        if (syncAfter) syncManager.syncNow(replaceLocal = true).getOrThrow()
    }

    /** Syncs now and tells what moved; [replaceLocal] takes the household's data (after joining). */
    suspend fun syncWithSummary(replaceLocal: Boolean = false): Result<SyncSummary> = syncManager.syncNow(replaceLocal)

    suspend fun createInvite(): Result<String> = cloudCall {
        val household = store.current().household ?: throw CloudException(CloudError.UNKNOWN)
        client.createInvite(requireSession(), household.id)
    }

    suspend fun members(): Result<List<HouseholdMember>> = cloudCall {
        val household = store.current().household ?: return@cloudCall emptyList()
        client.members(requireSession(), household.id)
    }

    suspend fun syncNow(): Result<Unit> = syncManager.syncNow().map { }

    /** The signed-in user's name from their cloud profile, or null. */
    suspend fun displayName(): String? = cloudCall { client.displayName(requireSession()) }.getOrNull()

    /** Stops syncing; the data stays on this device. */
    suspend fun signOut() = withContext(ioDispatcher) {
        store.current().session?.let { client.signOut(it.accessToken) }
        store.clearSession()
    }

    /** Leaves the household; the data stays on this device. */
    suspend fun leaveHousehold(): Result<Unit> = cloudCall {
        val household = store.current().household ?: return@cloudCall
        client.leaveHousehold(requireSession(), household.id)
        store.saveHousehold(null)
        store.saveLinksHouseholdId(null)
        linkDao.clear()
    }

    /** Saves the session and, if the user already belongs to a household, connects (and syncs) it. */
    private suspend fun connect(session: CloudSession, syncAfter: Boolean): Boolean {
        store.saveSession(session)
        val household = client.findMyHousehold(session) ?: return false
        syncManager.prepareFor(household.id)
        store.saveHousehold(household)
        if (syncAfter) syncManager.syncNow().getOrThrow()
        return true
    }

    private suspend fun requireSession(): CloudSession =
        syncManager.validSession() ?: throw CloudException(CloudError.SESSION_EXPIRED)

    private suspend fun <T> cloudCall(block: suspend () -> T): Result<T> = withContext(ioDispatcher) {
        runCatching { block() }.recoverCatching { throw SyncManager.toCloudException(it) }
    }
}
