package com.agcoding.networkapp.biometric.data.repository

import android.content.Context
import com.agcoding.networkapp.biometric.domain.repository.BiometricRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : BiometricRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isSecurityEnabled  = MutableStateFlow(prefs.getString(KEY_PIN_HASH, null) != null)
    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false))
    private val _hasSeenSetup       = MutableStateFlow(prefs.getBoolean(KEY_SETUP_SEEN, false))

    override fun isSecurityEnabled(): Flow<Boolean>  = _isSecurityEnabled
    override fun isBiometricEnabled(): Flow<Boolean> = _isBiometricEnabled
    override fun hasSeenSecuritySetup(): Flow<Boolean> = _hasSeenSetup

    override suspend fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hashPin(pin)).apply()
        _isSecurityEnabled.value = true
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return stored == hashPin(pin)
    }

    override suspend fun disableSecurity() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .putBoolean(KEY_SETUP_SEEN, false)
            .apply()
        _isSecurityEnabled.value  = false
        _isBiometricEnabled.value = false
        _hasSeenSetup.value       = false
    }

    override suspend fun markSecuritySetupSeen() {
        prefs.edit().putBoolean(KEY_SETUP_SEEN, true).apply()
        _hasSeenSetup.value = true
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest((pin + SALT).toByteArray()).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME            = "biometric_prefs"
        private const val KEY_PIN_HASH          = "pin_hash"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_SETUP_SEEN        = "setup_seen"
        private const val SALT                  = "com.agcoding.networkapp.pin.v1"
    }
}
