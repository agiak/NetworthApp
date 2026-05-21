package com.agcoding.networkapp.settings.presentation

import android.net.Uri
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme

sealed interface SettingsIntent {
    data object CancelImport : SettingsIntent
    data object ClearBackupResult : SettingsIntent
    data object ClearDeleteDataResult : SettingsIntent
    data object ClearDummyDataResult : SettingsIntent
    data object ConfirmImport : SettingsIntent
    data object DeleteData : SettingsIntent
    data object GenerateDummyData : SettingsIntent
    data object GenerateSpecificData : SettingsIntent
    data object DisableSecurity : SettingsIntent
    data object NavigateToFixedExpenses : SettingsIntent
    data object NavigateToOnboarding : SettingsIntent
    data object NavigateToProfileEdit : SettingsIntent
    data object NavigateToSetupPin : SettingsIntent
    data class ExportToUri(val uri: Uri) : SettingsIntent
    data class ExportCsvToUri(val uri: Uri) : SettingsIntent
    data class LoadImportFile(val uri: Uri) : SettingsIntent
    data class SetCurrency(val currency: AppCurrency) : SettingsIntent
    data class SetLanguage(val language: AppLanguage) : SettingsIntent
    data class SetTheme(val theme: AppTheme) : SettingsIntent
}
