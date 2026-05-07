package com.agcoding.networkapp.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import com.agcoding.networkapp.settings.domain.usecase.GenerateDummyDataUseCase
import com.agcoding.networkapp.settings.domain.usecase.GenerateSpecificDataUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetAppLanguageUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetAppThemeUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetUserProfileUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetAppLanguageUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetAppThemeUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val setAppThemeUseCase: SetAppThemeUseCase,
    private val setAppLanguageUseCase: SetAppLanguageUseCase,
    private val generateDummyDataUseCase: GenerateDummyDataUseCase,
    private val generateSpecificDataUseCase: GenerateSpecificDataUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val repository: NetWorthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.ActivityRestarted -> _uiState.update { it.copy(shouldRestartActivity = false) }
            SettingsIntent.ClearDeleteDataResult -> _uiState.update { it.copy(deleteDataResult = null) }
            SettingsIntent.ClearDummyDataResult -> _uiState.update { it.copy(dummyDataResult = null) }
            SettingsIntent.DeleteData -> deleteData()
            SettingsIntent.GenerateDummyData -> generateDummyData()
            SettingsIntent.GenerateSpecificData -> generateSpecificData()
            SettingsIntent.NavigateToProfileEdit -> { /* Handled in UI */ }
            is SettingsIntent.SetLanguage -> setLanguage(intent.language)
            is SettingsIntent.SetTheme -> setTheme(intent.theme)
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            getAppThemeUseCase().collect { theme -> _uiState.update { it.copy(appTheme = theme) } }
        }
        viewModelScope.launch {
            getAppLanguageUseCase().collect { language -> _uiState.update { it.copy(appLanguage = language) } }
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
        viewModelScope.launch(ioDispatcher) {
            setAppLanguageUseCase(language)
            _uiState.update { it.copy(shouldRestartActivity = true) }
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
