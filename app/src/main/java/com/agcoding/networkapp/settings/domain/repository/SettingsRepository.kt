package com.agcoding.networkapp.settings.domain.repository

import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getAppTheme(): Flow<AppTheme>
    fun getAppLanguage(): Flow<AppLanguage>
    fun getUserProfile(): Flow<UserProfile>
    fun isProfileCreated(): Flow<Boolean>
    suspend fun setAppTheme(theme: AppTheme)
    suspend fun setAppLanguage(language: AppLanguage)
    suspend fun setUserProfile(profile: UserProfile)
    suspend fun setProfileCreated(created: Boolean)
}
