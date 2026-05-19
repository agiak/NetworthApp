package com.agcoding.networkapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.agcoding.networkapp.account.presentation.AccountDetailScreen
import com.agcoding.networkapp.account.presentation.AccountSetupScreen
import com.agcoding.networkapp.account.presentation.AccountsOverviewScreen
import com.agcoding.networkapp.account.presentation.CreateAccountScreen
import com.agcoding.networkapp.analytics.presentation.AllMonthsScreen
import com.agcoding.networkapp.analytics.presentation.AnalyticsScreen
import com.agcoding.networkapp.analytics.presentation.prediction.PredictionScreen
import com.agcoding.networkapp.biometric.presentation.setup.SecuritySetupScreen
import com.agcoding.networkapp.biometric.presentation.setup.SecuritySetupViewModel
import com.agcoding.networkapp.compare.presentation.CompareScreen
import com.agcoding.networkapp.goal.presentation.GoalScreen
import com.agcoding.networkapp.history.presentation.EditEntryScreen
import com.agcoding.networkapp.history.presentation.EntryDetailsScreen
import com.agcoding.networkapp.history.presentation.HistoryScreen
import com.agcoding.networkapp.home.presentation.HomeScreen
import com.agcoding.networkapp.onboarding.OnboardingScreen
import com.agcoding.networkapp.onboarding.OnboardingViewModel
import com.agcoding.networkapp.recap.presentation.RecapScreen
import com.agcoding.networkapp.settings.presentation.ProfileScreen
import com.agcoding.networkapp.settings.presentation.ProfileTargetSetupScreen
import com.agcoding.networkapp.settings.presentation.SettingsScreen
import com.agcoding.networkapp.shared.navigation.AccountDetailRoute
import com.agcoding.networkapp.shared.navigation.AccountSetupRoute
import com.agcoding.networkapp.shared.navigation.AccountsRoute
import com.agcoding.networkapp.shared.navigation.AddSnapshotRoute
import com.agcoding.networkapp.shared.navigation.AllMonthsRoute
import com.agcoding.networkapp.shared.navigation.AnalyticsRoute
import com.agcoding.networkapp.shared.navigation.CompareRoute
import com.agcoding.networkapp.shared.navigation.CreateAccountRoute
import com.agcoding.networkapp.shared.navigation.EditEntryRoute
import com.agcoding.networkapp.shared.navigation.EntryDetailsRoute
import com.agcoding.networkapp.shared.navigation.GoalRoute
import com.agcoding.networkapp.shared.navigation.HistoryRoute
import com.agcoding.networkapp.shared.navigation.HomeRoute
import com.agcoding.networkapp.shared.navigation.OnboardingRoute
import com.agcoding.networkapp.shared.navigation.PredictionRoute
import com.agcoding.networkapp.shared.navigation.ProfileEditRoute
import com.agcoding.networkapp.shared.navigation.ProfileSetupRoute
import com.agcoding.networkapp.shared.navigation.ProfileTargetSetupRoute
import com.agcoding.networkapp.shared.navigation.RecapRoute
import com.agcoding.networkapp.shared.navigation.SecuritySetupRoute
import com.agcoding.networkapp.shared.navigation.SettingsRoute
import com.agcoding.networkapp.snapshot.AddSnapshotScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    isProfileCreated: Boolean,
    hasSeenSecuritySetup: Boolean,
    hasSeenOnboarding: Boolean,
) {
    // Frozen at first composition to prevent NavHost reset mid-session
    val startDestination = remember {
        when {
            !hasSeenOnboarding    -> OnboardingRoute()
            !isProfileCreated     -> ProfileSetupRoute
            !hasSeenSecuritySetup -> SecuritySetupRoute()
            else                  -> HomeRoute
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Profile Setup ──────────────────────────────────────────────────────
        composable<ProfileSetupRoute> {
            ProfileScreen(
                isSetup = true,
                onComplete = {
                    navController.navigate(ProfileTargetSetupRoute) {
                        popUpTo<ProfileSetupRoute> { inclusive = true }
                    }
                }
            )
        }
        composable<ProfileTargetSetupRoute> {
            ProfileTargetSetupScreen(
                onComplete = {
                    navController.navigate(AccountSetupRoute) {
                        popUpTo<ProfileTargetSetupRoute> { inclusive = true }
                    }
                }
            )
        }
        composable<AccountSetupRoute> {
            AccountSetupScreen(
                onAddAccount = { navController.navigate(CreateAccountRoute) },
                onContinue   = {
                    navController.navigate(SecuritySetupRoute()) {
                        popUpTo<AccountSetupRoute> { inclusive = true }
                    }
                }
            )
        }

        // ── Onboarding (first launch OR re-open from Settings) ─────────────────
        composable<OnboardingRoute> { entry ->
            val route: OnboardingRoute = entry.toRoute()
            val vm: OnboardingViewModel = hiltViewModel()

            LaunchedEffect(Unit) {
                vm.finished.collect {
                    if (route.fromSettings) {
                        navController.navigateUp()
                    } else {
                        navController.navigate(ProfileSetupRoute) {
                            popUpTo<OnboardingRoute> { inclusive = true }
                        }
                    }
                }
            }

            OnboardingScreen(
                onFinish = { vm.finish() },
                onSkip   = { vm.finish() },
            )
        }

        // ── Security Setup (first launch OR re-enable from Settings) ───────────
        composable<SecuritySetupRoute> { entry ->
            val route: SecuritySetupRoute = entry.toRoute()
            val vm: SecuritySetupViewModel = hiltViewModel()
            val state by vm.state.collectAsStateWithLifecycle()

            LaunchedEffect(state.isDone) {
                if (!state.isDone) return@LaunchedEffect
                if (route.skipPrompt) {
                    // Came from Settings → just go back
                    navController.navigateUp()
                } else {
                    // First-time → enter main app
                    navController.navigate(HomeRoute) {
                        popUpTo<SecuritySetupRoute> { inclusive = true }
                    }
                }
            }

            SecuritySetupScreen(state = state, onIntent = vm::onIntent)
        }

        // ── Main Screens ───────────────────────────────────────────────────────
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToHistory      = { navController.navigate(HistoryRoute) },
                onNavigateToProfileEdit  = { navController.navigate(ProfileEditRoute) },
                onNavigateToEntryDetails = { navController.navigate(EntryDetailsRoute(it)) },
                onNavigateToGoal         = { navController.navigate(GoalRoute) },
                onNavigateToAccounts     = {
                    // Treat as a tab switch so Home tab stays properly accessible afterward
                    navController.navigate(AccountsRoute) {
                        popUpTo<HomeRoute> { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToCreateAccount = { navController.navigate(CreateAccountRoute) },
            )
        }
        composable<AnalyticsRoute> {
            AnalyticsScreen(
                onNavigateToAllMonths  = { navController.navigate(AllMonthsRoute) },
                onNavigateToPrediction = { navController.navigate(PredictionRoute) },
                onNavigateToGoal      = { navController.navigate(GoalRoute) },
                onNavigateToRecap     = { navController.navigate(RecapRoute) },
                onNavigateToCompare   = { navController.navigate(CompareRoute) },
            )
        }
        composable<AllMonthsRoute>  { AllMonthsScreen(onNavigateBack = { navController.navigateUp() }) }
        composable<CompareRoute>    { CompareScreen(onNavigateBack = { navController.navigateUp() }) }
        composable<GoalRoute>       { GoalScreen(onNavigateBack = { navController.navigateUp() }) }
        composable<PredictionRoute> { PredictionScreen(onNavigateBack = { navController.navigateUp() }) }
        composable<RecapRoute>      { RecapScreen(onNavigateBack = { navController.navigateUp() }) }
        composable<AccountsRoute> {
            AccountsOverviewScreen(
                onNavigateToCreateAccount = { navController.navigate(CreateAccountRoute) },
                onNavigateToAccountDetail = { navController.navigate(AccountDetailRoute(it)) },
            )
        }
        composable<AccountDetailRoute> {
            AccountDetailScreen(onNavigateBack = { navController.navigateUp() })
        }
        composable<CreateAccountRoute> {
            CreateAccountScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable<HistoryRoute> {
            HistoryScreen(
                onNavigateBack          = { navController.navigateUp() },
                onNavigateToEntryDetails = { navController.navigate(EntryDetailsRoute(it)) },
            )
        }
        composable<EntryDetailsRoute> { entry ->
            val route: EntryDetailsRoute = entry.toRoute()
            EntryDetailsScreen(
                onNavigateBack  = { navController.navigateUp() },
                onNavigateToEdit = { navController.navigate(EditEntryRoute(route.entryId)) },
            )
        }
        composable<EditEntryRoute>  { EditEntryScreen(onNavigateBack = { navController.navigateUp() }) }

        composable<ProfileEditRoute> {
            ProfileScreen(
                isSetup   = false,
                onComplete = { navController.navigateUp() },
                onBack    = { navController.navigateUp() },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateToProfileEdit  = { navController.navigate(ProfileEditRoute) },
                onNavigateToSetupPin     = { navController.navigate(SecuritySetupRoute(skipPrompt = true)) },
                onNavigateToOnboarding   = { navController.navigate(OnboardingRoute(fromSettings = true)) },
            )
        }

        // ── Add Snapshot (via app shortcut) ────────────────────────────────────
        composable<AddSnapshotRoute> {
            AddSnapshotScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}
