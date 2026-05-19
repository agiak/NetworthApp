package com.agcoding.networkapp

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agcoding.networkapp.biometric.presentation.lock.LockScreen
import com.agcoding.networkapp.biometric.presentation.lock.LockViewModel
import com.agcoding.networkapp.navigation.NavGraph
import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.shared.navigation.AccountDetailRoute
import com.agcoding.networkapp.shared.navigation.AccountSetupRoute
import com.agcoding.networkapp.shared.navigation.AddSnapshotRoute
import com.agcoding.networkapp.shared.navigation.AllMonthsRoute
import com.agcoding.networkapp.shared.navigation.CompareRoute
import com.agcoding.networkapp.shared.navigation.CreateAccountRoute
import com.agcoding.networkapp.shared.navigation.EditEntryRoute
import com.agcoding.networkapp.shared.navigation.EntryDetailsRoute
import com.agcoding.networkapp.shared.navigation.GoalRoute
import com.agcoding.networkapp.shared.navigation.HistoryRoute
import com.agcoding.networkapp.shared.navigation.OnboardingRoute
import com.agcoding.networkapp.shared.navigation.PredictionRoute
import com.agcoding.networkapp.shared.navigation.ProfileEditRoute
import com.agcoding.networkapp.shared.navigation.ProfileSetupRoute
import com.agcoding.networkapp.shared.navigation.ProfileTargetSetupRoute
import com.agcoding.networkapp.shared.navigation.RecapRoute
import com.agcoding.networkapp.shared.navigation.SecuritySetupRoute
import com.agcoding.networkapp.shared.shortcut.ShortcutEvent
import com.agcoding.networkapp.shared.shortcut.ShortcutEventBus
import com.agcoding.networkapp.shared.ui.components.BottomNavigationBar
import com.agcoding.networkapp.shared.ui.theme.AppTheme

@Composable
fun NetWorthApp(appViewModel: AppViewModel = hiltViewModel()) {
    val appTheme             by appViewModel.appTheme.collectAsStateWithLifecycle()
    val appThemeVariant      by appViewModel.appThemeVariant.collectAsStateWithLifecycle()
    val appLanguage          by appViewModel.appLanguage.collectAsStateWithLifecycle()
    val isProfileCreated     by appViewModel.isProfileCreated.collectAsStateWithLifecycle()
    val isSecurityEnabled    by appViewModel.isSecurityEnabled.collectAsStateWithLifecycle()
    val hasSeenSecuritySetup by appViewModel.hasSeenSecuritySetup.collectAsStateWithLifecycle()
    val hasSeenOnboarding    by appViewModel.hasSeenOnboarding.collectAsStateWithLifecycle()
    val isAuthenticated      by appViewModel.isAuthenticated.collectAsStateWithLifecycle()

    LaunchedEffect(appLanguage) {
        val tag = when (appLanguage) {
            AppLanguage.ENGLISH -> "en"
            AppLanguage.GREEK   -> "el"
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    if (isProfileCreated == null) return

    val isDark = when (appTheme) {
        AppTheme.DARK   -> true
        AppTheme.LIGHT  -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    AppTheme(variant = appThemeVariant, isDark = isDark) {
        // Lock screen: only after setup is complete AND security is enabled AND not yet authenticated
        val requireLock = isProfileCreated == true &&
                          hasSeenSecuritySetup &&
                          isSecurityEnabled &&
                          !isAuthenticated

        if (requireLock) {
            val lockVm: LockViewModel = hiltViewModel()
            val lockState by lockVm.uiState.collectAsStateWithLifecycle()
            LockScreen(state = lockState, onIntent = lockVm::onIntent)
        } else {
            val navController = rememberNavController()
            val currentDest = navController.currentBackStackEntryAsState().value?.destination

            // Navigate to AddSnapshotRoute when the shortcut fires
            val pendingShortcut by ShortcutEventBus.pendingEvent.collectAsStateWithLifecycle()
            LaunchedEffect(pendingShortcut) {
                if (pendingShortcut == ShortcutEvent.AddSnapshot && isProfileCreated == true && hasSeenSecuritySetup) {
                    navController.navigate(AddSnapshotRoute)
                    ShortcutEventBus.consume()
                }
            }

            val showBottomBar = currentDest?.let { d ->
                !d.hasRoute<AccountDetailRoute>() &&
                !d.hasRoute<AccountSetupRoute>() &&
                !d.hasRoute<AddSnapshotRoute>() &&
                !d.hasRoute<AllMonthsRoute>() &&
                !d.hasRoute<CreateAccountRoute>() &&
                !d.hasRoute<CompareRoute>() &&
                !d.hasRoute<EditEntryRoute>() &&
                !d.hasRoute<EntryDetailsRoute>() &&
                !d.hasRoute<GoalRoute>() &&
                !d.hasRoute<HistoryRoute>() &&
                !d.hasRoute<OnboardingRoute>() &&
                !d.hasRoute<PredictionRoute>() &&
                !d.hasRoute<ProfileEditRoute>() &&
                !d.hasRoute<ProfileSetupRoute>() &&
                !d.hasRoute<ProfileTargetSetupRoute>() &&
                !d.hasRoute<RecapRoute>() &&
                !d.hasRoute<SecuritySetupRoute>()
            } ?: true

            Scaffold(
                bottomBar = { if (showBottomBar) BottomNavigationBar(navController) }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    NavGraph(
                        navController        = navController,
                        isProfileCreated     = isProfileCreated!!,
                        hasSeenOnboarding    = hasSeenOnboarding,
                        hasSeenSecuritySetup = hasSeenSecuritySetup,
                    )
                }
            }
        }
    }
}
