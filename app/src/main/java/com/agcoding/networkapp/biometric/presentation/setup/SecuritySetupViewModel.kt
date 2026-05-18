package com.agcoding.networkapp.biometric.presentation.setup

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.agcoding.networkapp.biometric.domain.auth.AuthStateManager
import com.agcoding.networkapp.biometric.domain.usecase.MarkSecuritySetupSeenUseCase
import com.agcoding.networkapp.biometric.domain.usecase.SetBiometricEnabledUseCase
import com.agcoding.networkapp.biometric.domain.usecase.SetPinUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import com.agcoding.networkapp.shared.navigation.SecuritySetupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SecurityStep { AskSecurity, EnterPin, ConfirmPin, AskBiometric, Success, Done }

data class SecuritySetupState(
    val step: SecurityStep = SecurityStep.AskSecurity,
    val pin: String = "",
    val firstPin: String = "",
    val pinMismatch: Boolean = false,
    val canUseBiometric: Boolean = false,
    val triggerBiometricPrompt: Boolean = false,
    val isDone: Boolean = false,
)

sealed interface SecuritySetupIntent {
    data object SetUpSecurity : SecuritySetupIntent
    data object SkipSecurity : SecuritySetupIntent
    data class DigitEntered(val digit: String) : SecuritySetupIntent
    data object Backspace : SecuritySetupIntent
    data object BiometricYes : SecuritySetupIntent
    data object BiometricNo : SecuritySetupIntent
    data object BiometricPromptShown : SecuritySetupIntent
    data object BiometricSuccess : SecuritySetupIntent
    data object BiometricFailed : SecuritySetupIntent
    data object LetsStart : SecuritySetupIntent
}

@HiltViewModel
class SecuritySetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val setPinUseCase: SetPinUseCase,
    private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
    private val markSecuritySetupSeenUseCase: MarkSecuritySetupSeenUseCase,
    private val authStateManager: AuthStateManager,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val skipPrompt = runCatching { savedStateHandle.toRoute<SecuritySetupRoute>().skipPrompt }.getOrDefault(false)

    private val _state = MutableStateFlow(
        SecuritySetupState(step = if (skipPrompt) SecurityStep.EnterPin else SecurityStep.AskSecurity)
    )
    val state: StateFlow<SecuritySetupState> = _state.asStateFlow()

    fun onIntent(intent: SecuritySetupIntent) {
        when (intent) {
            SecuritySetupIntent.SetUpSecurity     -> _state.update { it.copy(step = SecurityStep.EnterPin) }
            SecuritySetupIntent.SkipSecurity      -> skip()
            is SecuritySetupIntent.DigitEntered   -> addDigit(intent.digit)
            SecuritySetupIntent.Backspace         -> removeLastDigit()
            SecuritySetupIntent.BiometricYes      -> _state.update { it.copy(triggerBiometricPrompt = true) }
            SecuritySetupIntent.BiometricNo       -> goToSuccess(enableBiometric = false)
            SecuritySetupIntent.BiometricPromptShown -> _state.update { it.copy(triggerBiometricPrompt = false) }
            SecuritySetupIntent.BiometricSuccess  -> goToSuccess(enableBiometric = true)
            SecuritySetupIntent.BiometricFailed   -> goToSuccess(enableBiometric = false)
            SecuritySetupIntent.LetsStart         -> letsStart()
        }
    }

    private fun skip() {
        viewModelScope.launch(ioDispatcher) {
            markSecuritySetupSeenUseCase()
            _state.update { it.copy(isDone = true) }
        }
    }

    private fun addDigit(digit: String) {
        val current = _state.value.pin
        if (current.length >= PIN_LENGTH) return
        val newPin = current + digit
        _state.update { it.copy(pin = newPin, pinMismatch = false) }
        if (newPin.length == PIN_LENGTH) onPinComplete(newPin)
    }

    private fun removeLastDigit() {
        val current = _state.value.pin
        if (current.isEmpty()) return
        _state.update { it.copy(pin = current.dropLast(1), pinMismatch = false) }
    }

    private fun onPinComplete(pin: String) {
        when (_state.value.step) {
            SecurityStep.EnterPin -> {
                _state.update { it.copy(step = SecurityStep.ConfirmPin, firstPin = pin, pin = "") }
            }
            SecurityStep.ConfirmPin -> {
                if (pin == _state.value.firstPin) {
                    savePin(pin)
                } else {
                    _state.update { it.copy(pin = "", firstPin = "", step = SecurityStep.EnterPin, pinMismatch = true) }
                }
            }
            else -> Unit
        }
    }

    private fun savePin(pin: String) {
        viewModelScope.launch(ioDispatcher) {
            setPinUseCase(pin)
            val canBiometric = canUseBiometric()
            _state.update { it.copy(pin = "", step = SecurityStep.AskBiometric, canUseBiometric = canBiometric) }
        }
    }

    private fun goToSuccess(enableBiometric: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            setBiometricEnabledUseCase(enableBiometric)
            _state.update { it.copy(step = SecurityStep.Success, triggerBiometricPrompt = false) }
        }
    }

    private fun letsStart() {
        viewModelScope.launch(ioDispatcher) {
            markSecuritySetupSeenUseCase()
            authStateManager.markAuthenticated()
            _state.update { it.copy(isDone = true) }
        }
    }

    private fun canUseBiometric(): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS

    companion object {
        const val PIN_LENGTH = 4
    }
}
