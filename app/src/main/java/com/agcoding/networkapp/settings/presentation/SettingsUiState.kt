package com.agcoding.networkapp.settings.presentation

import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.model.UserProfile

sealed class DummyDataResult {
    data object Success : DummyDataResult()
    data class Failure(val cause: String?) : DummyDataResult()
}

data class SettingsUiState(
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val isDummyDataGenerating: Boolean = false,
    val dummyDataResult: DummyDataResult? = null,
    val shouldRestartActivity: Boolean = false,
    val snapshotCount: Int = 0,
    val trackingSince: String = "",
    val userProfile: UserProfile = UserProfile()
)
