package com.agcoding.networkapp.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.settings.domain.model.UserProfile
import com.agcoding.networkapp.settings.domain.usecase.GetUserProfileUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetUserProfileUseCase
import java.time.format.DateTimeFormatter
import java.util.Locale
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

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val target: String = "",
    val trackingSince: String = "",
    val isSaving: Boolean = false,
    val isComplete: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val setUserProfileUseCase: SetUserProfileUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = getUserProfileUseCase().first()
            val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
            _uiState.update {
                it.copy(
                    name = profile.name,
                    email = profile.email,
                    target = if (profile.targetAmount > 0.0) profile.targetAmount.toLong().toString() else "",
                    trackingSince = profile.createdAt?.format(formatter) ?: ""
                )
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }
    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email) }
    fun onTargetChange(target: String) = _uiState.update { it.copy(target = target) }

    fun onSave(isSetup: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSaving = true) }
            if (isSetup) {
                val currentProfile = getUserProfileUseCase().first()
                setUserProfileUseCase(currentProfile.copy(name = _uiState.value.name, email = _uiState.value.email))
            } else {
                val targetAmount = _uiState.value.target.toDoubleOrNull() ?: 0.0
                setUserProfileUseCase(UserProfile(name = _uiState.value.name, email = _uiState.value.email, targetAmount = targetAmount))
            }
            _uiState.update { it.copy(isSaving = false, isComplete = true) }
        }
    }
}
