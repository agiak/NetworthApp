package com.agcoding.networkapp.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.settings.domain.usecase.GetUserProfileUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetProfileCreatedUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetUserProfileUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileTargetSetupUiState(
    val target: String = "",
    val isSaving: Boolean = false,
    val isComplete: Boolean = false
)

@HiltViewModel
class ProfileTargetSetupViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val setUserProfileUseCase: SetUserProfileUseCase,
    private val setProfileCreatedUseCase: SetProfileCreatedUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileTargetSetupUiState())
    val uiState: StateFlow<ProfileTargetSetupUiState> = _uiState.asStateFlow()

    fun onTargetChange(target: String) = _uiState.update { it.copy(target = target) }

    fun onSave() {
        val targetValue = _uiState.value.target.toDoubleOrNull() ?: return
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSaving = true) }
            val currentProfile = getUserProfileUseCase().first()
            setUserProfileUseCase(currentProfile.copy(targetAmount = targetValue))
            setProfileCreatedUseCase(true)
            _uiState.update { it.copy(isSaving = false, isComplete = true) }
        }
    }
}
