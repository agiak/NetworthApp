package com.agcoding.networkapp.cloud.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.cloud.data.CloudRepository
import com.agcoding.networkapp.cloud.domain.model.CloudConnectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Online/offline status for Settings. */
@HiltViewModel
class CloudStatusViewModel @Inject constructor(
    private val repository: CloudRepository,
) : ViewModel() {

    val status: StateFlow<CloudConnectionStatus?> =
        repository.connectionStatus.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
