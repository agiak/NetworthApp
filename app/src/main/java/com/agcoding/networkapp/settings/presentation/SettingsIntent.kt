package com.agcoding.networkapp.settings.presentation

import android.net.Uri
import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme

sealed interface SettingsIntent {
    data object ActivityRestarted : SettingsIntent
    data object CancelImport : SettingsIntent
    data object ClearBackupResult : SettingsIntent
    data object ClearDeleteDataResult : SettingsIntent
    data object ClearDummyDataResult : SettingsIntent
    data object ConfirmImport : SettingsIntent
    data object DeleteData : SettingsIntent
    data object GenerateDummyData : SettingsIntent
    data object GenerateSpecificData : SettingsIntent
    data object NavigateToProfileEdit : SettingsIntent
    data class ExportToUri(val uri: Uri) : SettingsIntent
    data class LoadImportFile(val uri: Uri) : SettingsIntent
    data class SetLanguage(val language: AppLanguage) : SettingsIntent
    data class SetTheme(val theme: AppTheme) : SettingsIntent
}
