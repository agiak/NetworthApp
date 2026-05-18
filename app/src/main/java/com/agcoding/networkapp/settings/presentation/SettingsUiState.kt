package com.agcoding.networkapp.settings.presentation

import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.model.UserProfile

sealed class DummyDataResult {
    data object Success : DummyDataResult()
    data class Failure(val cause: String?) : DummyDataResult()
}

sealed class BackupResult {
    data object ExportSuccess : BackupResult()
    data object ImportSuccess : BackupResult()
    data object ImportInvalidFile : BackupResult()
    data class Failure(val cause: String?) : BackupResult()
}

data class SettingsUiState(
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val isSecurityEnabled: Boolean = false,
    val isDummyDataGenerating: Boolean = false,
    val dummyDataResult: DummyDataResult? = null,
    val isGeneratingSpecific: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteDataResult: DummyDataResult? = null,
    val snapshotCount: Int = 0,
    val trackingSince: String = "",
    val userProfile: UserProfile = UserProfile(),
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val backupResult: BackupResult? = null,
    val showImportConfirmDialog: Boolean = false,
    val pendingImportJson: String? = null
)
