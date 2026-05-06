package com.agcoding.networkapp.settings.data.repository

import android.content.Context
import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.model.UserProfile
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _appTheme = MutableStateFlow(readTheme())
    private val _appLanguage = MutableStateFlow(readLanguage())
    private val _userProfile = MutableStateFlow(readUserProfile())
    private val _isProfileCreated = MutableStateFlow(readIsProfileCreated())

    override fun getAppTheme(): Flow<AppTheme> = _appTheme
    override fun getAppLanguage(): Flow<AppLanguage> = _appLanguage
    override fun getUserProfile(): Flow<UserProfile> = _userProfile
    override fun isProfileCreated(): Flow<Boolean> = _isProfileCreated

    override suspend fun setAppTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.key).apply()
        _appTheme.value = theme
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.key).commit()
        _appLanguage.value = language
    }

    override suspend fun setUserProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_USER_EMAIL, profile.email)
            .putString(KEY_USER_NAME, profile.name)
            .putFloat(KEY_USER_TARGET, profile.targetAmount.toFloat())
            .apply()
        _userProfile.value = profile
    }

    override suspend fun setProfileCreated(created: Boolean) {
        prefs.edit().putBoolean(KEY_PROFILE_CREATED, created).apply()
        _isProfileCreated.value = created
    }

    private fun readTheme(): AppTheme {
        val key = prefs.getString(KEY_THEME, AppTheme.SYSTEM.key) ?: AppTheme.SYSTEM.key
        return AppTheme.entries.firstOrNull { it.key == key } ?: AppTheme.SYSTEM
    }

    private fun readLanguage(): AppLanguage {
        val key = prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.key) ?: AppLanguage.ENGLISH.key
        return AppLanguage.entries.firstOrNull { it.key == key } ?: AppLanguage.ENGLISH
    }

    private fun readUserProfile(): UserProfile {
        return UserProfile(
            name = prefs.getString(KEY_USER_NAME, "") ?: "",
            email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
            targetAmount = prefs.getFloat(KEY_USER_TARGET, 0f).toDouble()
        )
    }

    private fun readIsProfileCreated(): Boolean {
        return prefs.getBoolean(KEY_PROFILE_CREATED, false)
    }

    companion object {
        private const val KEY_LANGUAGE = "language"
        private const val KEY_PROFILE_CREATED = "is_profile_created"
        private const val KEY_THEME = "theme"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_TARGET = "user_target"
    }
}
