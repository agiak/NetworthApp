package com.agcoding.networkapp.biometric.presentation.lock

data class LockUiState(
    val pin: String = "",
    val pinError: Boolean = false,
    val showBiometricButton: Boolean = false,
    val triggerBiometricPrompt: Boolean = false,
    val isAuthenticated: Boolean = false,
)
