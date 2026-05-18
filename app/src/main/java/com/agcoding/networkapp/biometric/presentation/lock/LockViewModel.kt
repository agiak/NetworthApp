package com.agcoding.networkapp.biometric.presentation.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.biometric.domain.auth.AuthStateManager
import com.agcoding.networkapp.biometric.domain.usecase.IsBiometricEnabledUseCase
import com.agcoding.networkapp.biometric.domain.usecase.VerifyPinUseCase
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

@HiltViewModel
class LockViewModel @Inject constructor(
    private val verifyPinUseCase: VerifyPinUseCase,
    private val isBiometricEnabledUseCase: IsBiometricEnabledUseCase,
    private val authStateManager: AuthStateManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    fun onIntent(intent: LockIntent) {
        when (intent) {
            is LockIntent.Initialize      -> initialize(intent.deviceSupportsBiometric)
            is LockIntent.DigitEntered    -> addDigit(intent.digit)
            LockIntent.Backspace          -> removeLastDigit()
            LockIntent.BiometricRequested -> _uiState.update { it.copy(triggerBiometricPrompt = true) }
            LockIntent.BiometricSuccess   -> authenticate()
            LockIntent.BiometricFailed    -> _uiState.update { it.copy(triggerBiometricPrompt = false) }
            LockIntent.BiometricPromptShown -> _uiState.update { it.copy(triggerBiometricPrompt = false) }
        }
    }

    private fun initialize(deviceSupportsBiometric: Boolean) {
        viewModelScope.launch {
            val userEnabledBiometric = isBiometricEnabledUseCase().first()
            val canBiometric = deviceSupportsBiometric && userEnabledBiometric
            _uiState.update { it.copy(showBiometricButton = canBiometric, triggerBiometricPrompt = canBiometric) }
        }
    }

    private fun addDigit(digit: String) {
        val current = _uiState.value.pin
        if (current.length >= PIN_LENGTH) return
        val newPin = current + digit
        _uiState.update { it.copy(pin = newPin, pinError = false) }
        if (newPin.length == PIN_LENGTH) verifyPin(newPin)
    }

    private fun removeLastDigit() {
        val current = _uiState.value.pin
        if (current.isEmpty()) return
        _uiState.update { it.copy(pin = current.dropLast(1), pinError = false) }
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch(ioDispatcher) {
            if (verifyPinUseCase(pin)) authenticate()
            else _uiState.update { it.copy(pin = "", pinError = true) }
        }
    }

    private fun authenticate() {
        authStateManager.markAuthenticated()
        _uiState.update { it.copy(isAuthenticated = true, triggerBiometricPrompt = false) }
    }

    companion object {
        const val PIN_LENGTH = 4
    }
}
