package com.agcoding.networkapp.cloud.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agcoding.networkapp.cloud.domain.model.CloudSession
import com.agcoding.networkapp.cloud.domain.model.CloudState
import com.agcoding.networkapp.cloud.domain.model.Household
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cloudDataStore: DataStore<Preferences> by preferencesDataStore(name = "cloud_prefs")

/** Session tokens, the joined household and the last sync time, in app-private storage. */
@Singleton
class CloudSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = context.cloudDataStore

    val state: Flow<CloudState> = store.data.map { prefs ->
        val session = prefs[ACCESS_TOKEN]?.let { access ->
            CloudSession(
                accessToken  = access,
                refreshToken = prefs[REFRESH_TOKEN].orEmpty(),
                expiresAt    = prefs[EXPIRES_AT] ?: 0L,
                userId       = prefs[USER_ID].orEmpty(),
                email        = prefs[EMAIL].orEmpty(),
            )
        }
        val household = prefs[HOUSEHOLD_ID]?.let { id ->
            Household(id = id, name = prefs[HOUSEHOLD_NAME].orEmpty(), isOwner = prefs[HOUSEHOLD_ROLE] == "owner")
        }
        CloudState(session = session, household = household, lastSyncAt = prefs[LAST_SYNC_AT])
    }

    suspend fun current(): CloudState = state.first()

    suspend fun saveSession(session: CloudSession) {
        store.edit {
            it[ACCESS_TOKEN] = session.accessToken
            it[REFRESH_TOKEN] = session.refreshToken
            it[EXPIRES_AT] = session.expiresAt
            it[USER_ID] = session.userId
            it[EMAIL] = session.email
        }
    }

    suspend fun saveHousehold(household: Household?) {
        store.edit {
            if (household == null) {
                it.remove(HOUSEHOLD_ID); it.remove(HOUSEHOLD_NAME); it.remove(HOUSEHOLD_ROLE); it.remove(LAST_SYNC_AT)
            } else {
                it[HOUSEHOLD_ID] = household.id
                it[HOUSEHOLD_NAME] = household.name
                it[HOUSEHOLD_ROLE] = if (household.isOwner) "owner" else "member"
            }
        }
    }

    suspend fun saveLastSync(epochMillis: Long) {
        store.edit { it[LAST_SYNC_AT] = epochMillis }
    }

    /**
     * Signs out but keeps [linksHouseholdId] (signing in again resumes syncing where it left off),
     * pending changes and the sync reminder.
     */
    suspend fun clearSession() {
        store.edit { prefs ->
            val linksHousehold = prefs[LINKS_HOUSEHOLD_ID]
            val pending = prefs[PENDING_CHANGES]
            val snoozed = prefs[PROMPT_SNOOZED_UNTIL]
            prefs.clear()
            if (linksHousehold != null) prefs[LINKS_HOUSEHOLD_ID] = linksHousehold
            if (pending != null) prefs[PENDING_CHANGES] = pending
            if (snoozed != null) prefs[PROMPT_SNOOZED_UNTIL] = snoozed
        }
    }

    /** Local changes not yet uploaded, kept across restarts. */
    suspend fun hasPendingChanges(): Boolean = store.data.first()[PENDING_CHANGES] ?: false

    suspend fun savePendingChanges(pending: Boolean) {
        store.edit { it[PENDING_CHANGES] = pending }
    }

    /** Until when (epoch millis) the "sync your data" reminder stays hidden. */
    val promptSnoozedUntil: Flow<Long> = store.data.map { it[PROMPT_SNOOZED_UNTIL] ?: 0L }

    suspend fun snoozePrompt(untilEpochMillis: Long) {
        store.edit { it[PROMPT_SNOOZED_UNTIL] = untilEpochMillis }
    }

    /** The household the local sync links point to. */
    suspend fun linksHouseholdId(): String? = store.data.first()[LINKS_HOUSEHOLD_ID]

    suspend fun saveLinksHouseholdId(householdId: String?) {
        store.edit { if (householdId == null) it.remove(LINKS_HOUSEHOLD_ID) else it[LINKS_HOUSEHOLD_ID] = householdId }
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val EXPIRES_AT = longPreferencesKey("expires_at")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val HOUSEHOLD_ID = stringPreferencesKey("household_id")
        val HOUSEHOLD_NAME = stringPreferencesKey("household_name")
        val HOUSEHOLD_ROLE = stringPreferencesKey("household_role")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val LINKS_HOUSEHOLD_ID = stringPreferencesKey("links_household_id")
        val PENDING_CHANGES = booleanPreferencesKey("pending_changes")
        val PROMPT_SNOOZED_UNTIL = longPreferencesKey("prompt_snoozed_until")
    }
}
