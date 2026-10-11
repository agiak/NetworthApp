package com.agcoding.networkapp.cloud.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.cloud.data.CloudRepository
import com.agcoding.networkapp.cloud.domain.model.CloudConnectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Online/offline status for Settings and the "sync your data" reminder. */
@HiltViewModel
class CloudStatusViewModel @Inject constructor(
    private val repository: CloudRepository,
) : ViewModel() {

    val status: StateFlow<CloudConnectionStatus?> =
        repository.connectionStatus.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val showSyncPrompt: StateFlow<Boolean> =
        repository.shouldPromptSync.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** "Later": ask again in a few days. */
    fun remindLater() {
        viewModelScope.launch { repository.snoozeSyncPrompt(REMIND_LATER_MILLIS) }
    }

    /** Going to set it up now: don't ask again today even if it's left half way. */
    fun startSetup() {
        viewModelScope.launch { repository.snoozeSyncPrompt(AFTER_SETUP_MILLIS) }
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
        const val REMIND_LATER_MILLIS = 3 * DAY
        const val AFTER_SETUP_MILLIS = DAY
    }
}
