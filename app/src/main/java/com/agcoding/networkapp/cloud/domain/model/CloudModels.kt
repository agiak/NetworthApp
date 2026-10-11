package com.agcoding.networkapp.cloud.domain.model

data class CloudSession(
    val accessToken: String,
    val refreshToken: String,
    /** Epoch seconds. */
    val expiresAt: Long,
    val userId: String,
    val email: String,
    /** Signed in with a temporary password: a new one has to be chosen before going on. */
    val mustChangePassword: Boolean = false,
) {
    fun expiresSoon(nowSeconds: Long): Boolean = expiresAt - nowSeconds < REFRESH_MARGIN_SECONDS

    companion object {
        private const val REFRESH_MARGIN_SECONDS = 60L
    }
}

data class Household(
    val id: String,
    val name: String,
    val isOwner: Boolean,
)

data class HouseholdMember(
    val userId: String,
    val displayName: String,
    val isOwner: Boolean,
    val isMe: Boolean,
)

/** Everything the app remembers about the cloud connection. */
data class CloudState(
    val session: CloudSession? = null,
    val household: Household? = null,
    /** Epoch millis of the last successful sync. */
    val lastSyncAt: Long? = null,
) {
    val isSignedIn: Boolean get() = session != null
    val isSyncing: Boolean get() = session != null && household != null
}

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Failed(val message: String) : SyncStatus
}

/** How this phone stands with the cloud, for the Settings screen. */
enum class CloudMode {
    /** Not set up: the data lives only on this phone. */
    OFFLINE_MODE,
    /** Signed in but not in a household yet. */
    SETUP_INCOMPLETE,
    ONLINE,
    SYNCING,
    /** Set up, but the phone has no internet right now. */
    NO_INTERNET,
    SYNC_ERROR,
}

data class CloudConnectionStatus(
    val mode: CloudMode,
    val lastSyncAt: Long? = null,
    /** Changes made here that haven't reached the cloud yet. */
    val hasPendingChanges: Boolean = false,
) {
    val isConnected: Boolean get() = mode != CloudMode.OFFLINE_MODE && mode != CloudMode.SETUP_INCOMPLETE
}

fun cloudConnectionStatus(
    state: CloudState,
    syncStatus: SyncStatus,
    isOnline: Boolean,
    hasPendingChanges: Boolean,
): CloudConnectionStatus {
    val mode = when {
        state.session == null      -> CloudMode.OFFLINE_MODE
        state.household == null    -> CloudMode.SETUP_INCOMPLETE
        !isOnline                  -> CloudMode.NO_INTERNET
        syncStatus is SyncStatus.Syncing -> CloudMode.SYNCING
        syncStatus is SyncStatus.Failed  -> CloudMode.SYNC_ERROR
        else                       -> CloudMode.ONLINE
    }
    return CloudConnectionStatus(
        mode              = mode,
        lastSyncAt        = state.lastSyncAt,
        hasPendingChanges = hasPendingChanges && state.isSyncing,
    )
}

/** The steps of a sync, in order, for the progress screen. */
enum class SyncStage { CONNECTING, CHECKING, ACCOUNTS, ENTRIES, EXPENSES, CATEGORIES, FINISHING, DONE }

data class SyncProgress(
    val stage: SyncStage,
    /** 0.0–1.0 for the whole sync. */
    val fraction: Float,
)

/** What a sync moved, per table. */
data class SyncSummary(
    val accountsUploaded: Int = 0,
    val entriesUploaded: Int = 0,
    val expensesUploaded: Int = 0,
    val categoriesUploaded: Int = 0,
    // Rows added, changed or deleted on this phone from the household
    val accountsReceived: Int = 0,
    val entriesReceived: Int = 0,
    val expensesReceived: Int = 0,
    val categoriesReceived: Int = 0,
) {
    val uploaded: Int get() = accountsUploaded + entriesUploaded + expensesUploaded + categoriesUploaded
    val received: Int get() = accountsReceived + entriesReceived + expensesReceived + categoriesReceived
}
