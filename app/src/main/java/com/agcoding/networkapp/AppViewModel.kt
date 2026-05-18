package com.agcoding.networkapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.biometric.domain.auth.AuthStateManager
import com.agcoding.networkapp.biometric.domain.usecase.HasSeenSecuritySetupUseCase
import com.agcoding.networkapp.biometric.domain.usecase.IsSecurityEnabledUseCase
import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.usecase.GetAppLanguageUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetAppThemeUseCase
import com.agcoding.networkapp.settings.domain.usecase.HasSeenOnboardingUseCase
import com.agcoding.networkapp.settings.domain.usecase.IsProfileCreatedUseCase
import com.agcoding.networkapp.shared.domain.preferences.ThemePreferencesRepository
import com.agcoding.networkapp.shared.ui.theme.AppThemeVariant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val getAppThemeUseCase: GetAppThemeUseCase,
    private val getAppLanguageUseCase: GetAppLanguageUseCase,
    private val isProfileCreatedUseCase: IsProfileCreatedUseCase,
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val isSecurityEnabledUseCase: IsSecurityEnabledUseCase,
    private val hasSeenSecuritySetupUseCase: HasSeenSecuritySetupUseCase,
    private val hasSeenOnboardingUseCase: HasSeenOnboardingUseCase,
    private val authStateManager: AuthStateManager,
) : ViewModel() {

    private val _appTheme = MutableStateFlow(AppTheme.SYSTEM)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _appThemeVariant = MutableStateFlow(AppThemeVariant.Default)
    val appThemeVariant: StateFlow<AppThemeVariant> = _appThemeVariant.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _isProfileCreated = MutableStateFlow<Boolean?>(null)
    val isProfileCreated: StateFlow<Boolean?> = _isProfileCreated.asStateFlow()

    private val _isSecurityEnabled = MutableStateFlow(false)
    val isSecurityEnabled: StateFlow<Boolean> = _isSecurityEnabled.asStateFlow()

    private val _hasSeenSecuritySetup = MutableStateFlow(false)
    val hasSeenSecuritySetup: StateFlow<Boolean> = _hasSeenSecuritySetup.asStateFlow()

    private val _hasSeenOnboarding = MutableStateFlow(false)
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

    val isAuthenticated: StateFlow<Boolean> = authStateManager.isAuthenticated

    init {
        viewModelScope.launch { getAppThemeUseCase().collect { _appTheme.value = it } }
        viewModelScope.launch { themePreferencesRepository.observeVariant().collect { _appThemeVariant.value = it } }
        viewModelScope.launch { getAppLanguageUseCase().collect { _appLanguage.value = it } }
        viewModelScope.launch { isProfileCreatedUseCase().collect { _isProfileCreated.value = it } }
        viewModelScope.launch { isSecurityEnabledUseCase().collect { _isSecurityEnabled.value = it } }
        viewModelScope.launch { hasSeenSecuritySetupUseCase().collect { _hasSeenSecuritySetup.value = it } }
        viewModelScope.launch { hasSeenOnboardingUseCase().collect { _hasSeenOnboarding.value = it } }
    }
}
