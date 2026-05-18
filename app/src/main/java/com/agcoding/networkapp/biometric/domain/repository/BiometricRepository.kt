package com.agcoding.networkapp.biometric.domain.repository

import kotlinx.coroutines.flow.Flow

interface BiometricRepository {
    fun isSecurityEnabled(): Flow<Boolean>
    fun isBiometricEnabled(): Flow<Boolean>
    fun hasSeenSecuritySetup(): Flow<Boolean>
    suspend fun setPin(pin: String)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun verifyPin(pin: String): Boolean
    suspend fun disableSecurity()
    suspend fun markSecuritySetupSeen()
}
