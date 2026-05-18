package com.agcoding.networkapp.biometric.presentation.lock

sealed interface LockIntent {
    data class Initialize(val deviceSupportsBiometric: Boolean) : LockIntent
    data class DigitEntered(val digit: String) : LockIntent
    data object Backspace : LockIntent
    data object BiometricRequested : LockIntent
    data object BiometricSuccess : LockIntent
    data object BiometricFailed : LockIntent
    data object BiometricPromptShown : LockIntent
}
