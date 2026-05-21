package com.agcoding.networkapp.settings.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.backup.domain.usecase.ExportCsvUseCase
import com.agcoding.networkapp.backup.domain.usecase.ExportDataUseCase
import com.agcoding.networkapp.backup.domain.usecase.ImportDataUseCase
import com.agcoding.networkapp.biometric.domain.usecase.DisableSecurityUseCase
import com.agcoding.networkapp.biometric.domain.usecase.IsSecurityEnabledUseCase
import com.agcoding.networkapp.fixedexpenses.domain.usecase.GetFixedExpensesYearlySummaryUseCase
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import com.agcoding.networkapp.settings.domain.usecase.GenerateDummyDataUseCase
import com.agcoding.networkapp.settings.domain.usecase.GenerateSpecificDataUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetAppLanguageUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetAppThemeUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetUserProfileUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetAppCurrencyUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetAppLanguageUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetAppThemeUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getAppThemeUseCase: GetAppThemeUseCase,
    private val getAppLanguageUseCase: GetAppLanguageUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val setAppThemeUseCase: SetAppThemeUseCase,
    private val setAppLanguageUseCase: SetAppLanguageUseCase,
    private val setAppCurrencyUseCase: SetAppCurrencyUseCase,
    private val generateDummyDataUseCase: GenerateDummyDataUseCase,
    private val generateSpecificDataUseCase: GenerateSpecificDataUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val repository: NetWorthRepository,
    private val exportDataUseCase: ExportDataUseCase,
    private val exportCsvUseCase: ExportCsvUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val isSecurityEnabledUseCase: IsSecurityEnabledUseCase,
    private val disableSecurityUseCase: DisableSecurityUseCase,
    private val getFixedExpensesYearlySummaryUseCase: GetFixedExpensesYearlySummaryUseCase,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.CancelImport -> _uiState.update { it.copy(showImportConfirmDialog = false, pendingImportJson = null) }
            SettingsIntent.ClearBackupResult -> _uiState.update { it.copy(backupResult = null) }
            SettingsIntent.ClearDeleteDataResult -> _uiState.update { it.copy(deleteDataResult = null) }
            SettingsIntent.ClearDummyDataResult -> _uiState.update { it.copy(dummyDataResult = null) }
            SettingsIntent.ConfirmImport -> confirmImport()
            SettingsIntent.DeleteData -> deleteData()
            SettingsIntent.GenerateDummyData -> generateDummyData()
            SettingsIntent.GenerateSpecificData -> generateSpecificData()
            SettingsIntent.DisableSecurity -> disableSecurity()
            SettingsIntent.NavigateToFixedExpenses -> { /* Handled in UI */ }
            SettingsIntent.NavigateToOnboarding -> { /* Handled in UI */ }
            SettingsIntent.NavigateToProfileEdit -> { /* Handled in UI */ }
            SettingsIntent.NavigateToSetupPin -> { /* Handled in UI */ }
            is SettingsIntent.ExportToUri    -> exportToUri(intent.uri)
            is SettingsIntent.ExportCsvToUri -> exportCsvToUri(intent.uri)
            is SettingsIntent.LoadImportFile -> loadImportFile(intent.uri)
            is SettingsIntent.SetCurrency -> setCurrency(intent.currency)
            is SettingsIntent.SetLanguage -> setLanguage(intent.language)
            is SettingsIntent.SetTheme -> setTheme(intent.theme)
        }
    }

    private fun disableSecurity() {
        viewModelScope.launch(ioDispatcher) { disableSecurityUseCase() }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            getAppThemeUseCase().collect { theme -> _uiState.update { it.copy(appTheme = theme) } }
        }
        viewModelScope.launch {
            isSecurityEnabledUseCase().collect { enabled -> _uiState.update { it.copy(isSecurityEnabled = enabled) } }
        }
        viewModelScope.launch {
            getAppLanguageUseCase().collect { language -> _uiState.update { it.copy(appLanguage = language) } }
        }
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currency ->
                _uiState.update { it.copy(appCurrency = currency) }
            }
        }
        viewModelScope.launch {
            combine(
                getFixedExpensesYearlySummaryUseCase(),
                getAppCurrencyUseCase(),
            ) { yearly, currency ->
                if (yearly > 0) "${currency.symbol}${String.format(Locale.US, "%,.0f", yearly)} / yr" else ""
            }.collect { formatted ->
                _uiState.update { it.copy(fixedExpensesYearlySummary = formatted) }
            }
        }
        viewModelScope.launch {
            getUserProfileUseCase().collect { profile -> _uiState.update { it.copy(userProfile = profile) } }
        }
        viewModelScope.launch {
            repository.getEntries().collect { result ->
                result.onSuccess { entries ->
                    val count = entries.size
                    val since = entries.minByOrNull { it.date }?.date?.let {
                        val month = it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        "$month ${it.year}"
                    } ?: "---"
                    _uiState.update { it.copy(snapshotCount = count, trackingSince = since) }
                }
            }
        }
    }

    private fun setTheme(theme: com.agcoding.networkapp.settings.domain.model.AppTheme) {
        viewModelScope.launch(ioDispatcher) { setAppThemeUseCase(theme) }
    }

    private fun setLanguage(language: com.agcoding.networkapp.settings.domain.model.AppLanguage) {
        viewModelScope.launch(ioDispatcher) { setAppLanguageUseCase(language) }
    }

    private fun setCurrency(currency: com.agcoding.networkapp.settings.domain.model.AppCurrency) {
        viewModelScope.launch(ioDispatcher) { setAppCurrencyUseCase(currency) }
    }

    private fun exportCsvToUri(uri: Uri) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isExportingCsv = true) }
            try {
                val csv = exportCsvUseCase()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(csv.toByteArray(Charsets.UTF_8))
                }
                _uiState.update { it.copy(isExportingCsv = false, backupResult = BackupResult.ExportSuccess) }
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(isExportingCsv = false, backupResult = BackupResult.Failure(e.message)) }
            }
        }
    }

    private fun exportToUri(uri: Uri) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val json = exportDataUseCase()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                _uiState.update { it.copy(isExporting = false, backupResult = BackupResult.ExportSuccess) }
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(isExporting = false, backupResult = BackupResult.Failure(e.message)) }
            }
        }
    }

    private fun loadImportFile(uri: Uri) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                } ?: run {
                    _uiState.update { it.copy(backupResult = BackupResult.ImportInvalidFile) }
                    return@launch
                }
                if (!importDataUseCase.validate(json)) {
                    _uiState.update { it.copy(backupResult = BackupResult.ImportInvalidFile) }
                    return@launch
                }
                _uiState.update { it.copy(pendingImportJson = json, showImportConfirmDialog = true) }
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update { it.copy(backupResult = BackupResult.Failure(e.message)) }
            }
        }
    }

    private fun confirmImport() {
        val json = _uiState.value.pendingImportJson ?: return
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isImporting = true, showImportConfirmDialog = false) }
            importDataUseCase(json).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isImporting = false, pendingImportJson = null, backupResult = BackupResult.ImportSuccess)
                    }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update {
                        it.copy(isImporting = false, pendingImportJson = null, backupResult = BackupResult.Failure(error.message))
                    }
                }
            )
        }
    }

    private fun generateSpecificData() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isGeneratingSpecific = true) }
            generateSpecificDataUseCase().fold(
                onSuccess = {
                    _uiState.update { it.copy(isGeneratingSpecific = false, dummyDataResult = DummyDataResult.Success) }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(isGeneratingSpecific = false, dummyDataResult = DummyDataResult.Failure(error.message)) }
                }
            )
        }
    }

    private fun deleteData() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isDeleting = true) }
            repository.deleteAllEntries().fold(
                onSuccess = {
                    _uiState.update { it.copy(isDeleting = false, deleteDataResult = DummyDataResult.Success) }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(isDeleting = false, deleteDataResult = DummyDataResult.Failure(error.message)) }
                }
            )
        }
    }

    private fun generateDummyData() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isDummyDataGenerating = true) }
            generateDummyDataUseCase().fold(
                onSuccess = {
                    _uiState.update { it.copy(isDummyDataGenerating = false, dummyDataResult = DummyDataResult.Success) }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(isDummyDataGenerating = false, dummyDataResult = DummyDataResult.Failure(error.message)) }
                }
            )
        }
    }
}
