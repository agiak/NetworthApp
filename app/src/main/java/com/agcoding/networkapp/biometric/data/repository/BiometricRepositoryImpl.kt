package com.agcoding.networkapp.biometric.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.agcoding.networkapp.biometric.domain.repository.BiometricRepository
import com.agcoding.networkapp.shared.di.BiometricDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricRepositoryImpl @Inject constructor(
    @BiometricDataStore private val dataStore: DataStore<Preferences>,
) : BiometricRepository {

    override fun isSecurityEnabled(): Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[KEY_PIN_HASH] != null }

    override fun isBiometricEnabled(): Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[KEY_BIOMETRIC_ENABLED] ?: false }

    override fun hasSeenSecuritySetup(): Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[KEY_SETUP_SEEN] ?: false }

    override suspend fun setPin(pin: String) {
        dataStore.edit { prefs -> prefs[KEY_PIN_HASH] = hashPin(pin) }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
        val stored = prefs[KEY_PIN_HASH] ?: return false
        return stored == hashPin(pin)
    }

    override suspend fun disableSecurity() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
            prefs[KEY_BIOMETRIC_ENABLED] = false
            prefs[KEY_SETUP_SEEN] = false
        }
    }

    override suspend fun markSecuritySetupSeen() {
        dataStore.edit { prefs -> prefs[KEY_SETUP_SEEN] = true }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest((pin + SALT).toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_SETUP_SEEN = booleanPreferencesKey("setup_seen")
        const val SALT = "com.agcoding.networkapp.pin.v1"
    }
}
