package com.agcoding.networkapp.settings.presentation

import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme

sealed interface SettingsIntent {
    data object ActivityRestarted : SettingsIntent
    data object ClearDeleteDataResult : SettingsIntent
    data object ClearDummyDataResult : SettingsIntent
    data object DeleteData : SettingsIntent
    data object GenerateDummyData : SettingsIntent
    data object GenerateSpecificData : SettingsIntent
    data object NavigateToProfileEdit : SettingsIntent
    data class SetLanguage(val language: AppLanguage) : SettingsIntent
    data class SetTheme(val theme: AppTheme) : SettingsIntent
}
