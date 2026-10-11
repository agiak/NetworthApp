package com.agcoding.networkapp.cloud.data

import com.agcoding.networkapp.cloud.data.local.SyncLinkDao
import com.agcoding.networkapp.cloud.data.remote.SupabaseClient
import com.agcoding.networkapp.cloud.data.session.CloudSessionStore
import com.agcoding.networkapp.cloud.data.sync.SyncManager
import com.agcoding.networkapp.cloud.domain.model.CloudError
import com.agcoding.networkapp.cloud.domain.model.CloudException
import com.agcoding.networkapp.cloud.domain.model.CloudSession
import com.agcoding.networkapp.cloud.domain.model.CloudState
import com.agcoding.networkapp.cloud.domain.model.HouseholdMember
import com.agcoding.networkapp.cloud.domain.model.SignUpResult
import com.agcoding.networkapp.cloud.domain.model.SyncStatus
import com.agcoding.networkapp.shared.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    val state: Flow<CloudState> = store.state
    val syncStatus: StateFlow<SyncStatus> = syncManager.status

    suspend fun signIn(email: String, password: String): Result<Unit> = cloudCall {
        val session = client.signIn(email.trim(), password)
        connect(session)
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<SignUpResult> = cloudCall {
        val session = client.signUp(email.trim(), password, displayName.trim())
            ?: return@cloudCall SignUpResult.ConfirmEmail
        connect(session)
        SignUpResult.SignedIn
    }

    /** Creates a household and uploads this device's data to it. */
    suspend fun createHousehold(name: String): Result<Unit> = cloudCall {
        val session = requireSession()
        val household = client.createHousehold(session, name.trim())
        syncManager.prepareFor(household.id)
        store.saveHousehold(household)
        syncManager.syncNow().getOrThrow()
    }

    /** Joins with an invite code; this device's data is replaced with the household's. */
    suspend fun joinHousehold(code: String): Result<Unit> = cloudCall {
        val session = requireSession()
        val household = client.joinHousehold(session, code.trim())
        syncManager.prepareFor(household.id)
        store.saveHousehold(household)
        syncManager.syncNow(replaceLocal = true).getOrThrow()
    }

    suspend fun createInvite(): Result<String> = cloudCall {
        val household = store.current().household ?: throw CloudException(CloudError.UNKNOWN)
        client.createInvite(requireSession(), household.id)
    }

    suspend fun members(): Result<List<HouseholdMember>> = cloudCall {
        val household = store.current().household ?: return@cloudCall emptyList()
        client.members(requireSession(), household.id)
    }

    suspend fun syncNow(): Result<Unit> = syncManager.syncNow()

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

    /** Saves the session and, if the user already belongs to a household, starts syncing it. */
    private suspend fun connect(session: CloudSession) {
        store.saveSession(session)
        val household = client.findMyHousehold(session)
        if (household != null) {
            syncManager.prepareFor(household.id)
            store.saveHousehold(household)
            syncManager.syncNow().getOrThrow()
        }
    }

    private suspend fun requireSession(): CloudSession =
        syncManager.validSession() ?: throw CloudException(CloudError.SESSION_EXPIRED)

    private suspend fun <T> cloudCall(block: suspend () -> T): Result<T> = withContext(ioDispatcher) {
        runCatching { block() }.recoverCatching { throw SyncManager.toCloudException(it) }
    }
}
