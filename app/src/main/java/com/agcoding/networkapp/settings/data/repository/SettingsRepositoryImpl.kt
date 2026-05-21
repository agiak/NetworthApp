package com.agcoding.networkapp.settings.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.model.UserProfile
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import com.agcoding.networkapp.shared.di.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @SettingsDataStore private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override fun getAppTheme(): Flow<AppTheme> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val key = prefs[Keys.THEME] ?: AppTheme.SYSTEM.key
            AppTheme.entries.firstOrNull { it.key == key } ?: AppTheme.SYSTEM
        }

    override fun getAppLanguage(): Flow<AppLanguage> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val key = prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.key
            AppLanguage.entries.firstOrNull { it.key == key } ?: AppLanguage.ENGLISH
        }

    override fun getAppCurrency(): Flow<AppCurrency> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val key = prefs[Keys.CURRENCY] ?: AppCurrency.EUR.key
            AppCurrency.entries.firstOrNull { it.key == key } ?: AppCurrency.EUR
        }

    override fun getUserProfile(): Flow<UserProfile> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val epochDay = prefs[Keys.CREATED_AT] ?: 0L
            UserProfile(
                name = prefs[Keys.USER_NAME] ?: "",
                email = prefs[Keys.USER_EMAIL] ?: "",
                targetAmount = (prefs[Keys.USER_TARGET] ?: 0f).toDouble(),
                createdAt = if (epochDay > 0L) LocalDate.ofEpochDay(epochDay) else null
            )
        }

    override fun isProfileCreated(): Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.PROFILE_CREATED] ?: false }

    override fun hasSeenOnboarding(): Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.ONBOARDING_SEEN] ?: false }

    override suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { prefs -> prefs[Keys.THEME] = theme.key }
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { prefs -> prefs[Keys.LANGUAGE] = language.key }
    }

    override suspend fun setAppCurrency(currency: AppCurrency) {
        dataStore.edit { prefs -> prefs[Keys.CURRENCY] = currency.key }
    }

    override suspend fun setUserProfile(profile: UserProfile) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_EMAIL] = profile.email
            prefs[Keys.USER_NAME] = profile.name
            prefs[Keys.USER_TARGET] = profile.targetAmount.toFloat()
            if (!prefs.contains(Keys.CREATED_AT) && profile.createdAt != null) {
                prefs[Keys.CREATED_AT] = profile.createdAt.toEpochDay()
            }
        }
    }

    override suspend fun setProfileCreated(created: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.PROFILE_CREATED] = created }
    }

    override suspend fun markOnboardingSeen() {
        dataStore.edit { prefs -> prefs[Keys.ONBOARDING_SEEN] = true }
    }

    private object Keys {
        val CREATED_AT = longPreferencesKey("created_at")
        val CURRENCY = stringPreferencesKey("currency")
        val LANGUAGE = stringPreferencesKey("language")
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
        val PROFILE_CREATED = booleanPreferencesKey("is_profile_created")
        val THEME = stringPreferencesKey("theme")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_TARGET = floatPreferencesKey("user_target")
    }
}
