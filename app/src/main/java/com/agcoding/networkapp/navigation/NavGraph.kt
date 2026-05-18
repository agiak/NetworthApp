package com.agcoding.networkapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.agcoding.networkapp.analytics.presentation.AllMonthsScreen
import com.agcoding.networkapp.analytics.presentation.AnalyticsScreen
import com.agcoding.networkapp.analytics.presentation.prediction.PredictionScreen
import com.agcoding.networkapp.compare.presentation.CompareScreen
import com.agcoding.networkapp.goal.presentation.GoalScreen
import com.agcoding.networkapp.history.presentation.EditEntryScreen
import com.agcoding.networkapp.history.presentation.EntryDetailsScreen
import com.agcoding.networkapp.history.presentation.HistoryScreen
import com.agcoding.networkapp.home.presentation.HomeScreen
import com.agcoding.networkapp.recap.presentation.RecapScreen
import com.agcoding.networkapp.settings.presentation.ProfileScreen
import com.agcoding.networkapp.settings.presentation.ProfileTargetSetupScreen
import com.agcoding.networkapp.settings.presentation.SettingsScreen
import com.agcoding.networkapp.shared.navigation.Screen

@Composable
fun NavGraph(
    navController: NavHostController,
    isProfileCreated: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = if (isProfileCreated) Screen.Home.route else Screen.ProfileSetup.route
    ) {
        composable(Screen.ProfileSetup.route) {
            ProfileScreen(
                isSetup = true,
                onComplete = {
                    navController.navigate(Screen.ProfileTargetSetup.route) {
                        popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ProfileTargetSetup.route) {
            ProfileTargetSetupScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ProfileTargetSetup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ProfileEdit.route) {
            ProfileScreen(
                isSetup = false,
                onComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToProfileEdit = { navController.navigate(Screen.ProfileEdit.route) },
                onNavigateToEntryDetails = { entryId ->
                    navController.navigate(Screen.EntryDetails.createRoute(entryId))
                },
                onNavigateToGoal = { navController.navigate(Screen.Goal.route) }
            )
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onNavigateToAllMonths = { navController.navigate(Screen.AllMonths.route) },
                onNavigateToPrediction = { navController.navigate(Screen.Prediction.route) },
                onNavigateToGoal = { navController.navigate(Screen.Goal.route) },
                onNavigateToRecap = { navController.navigate(Screen.Recap.route) },
                onNavigateToCompare = { navController.navigate(Screen.Compare.route) }
            )
        }
        composable(Screen.Compare.route) {
            CompareScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Recap.route) {
            RecapScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.AllMonths.route) {
            AllMonthsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Prediction.route) {
            PredictionScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Goal.route) {
            GoalScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Accounts.route) { /* TODO: Accounts feature */ }
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntryDetails = { entryId ->
                    navController.navigate(Screen.EntryDetails.createRoute(entryId))
                }
            )
        }
        composable(
            route = Screen.EntryDetails.route,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) {
            EntryDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { entryId ->
                    navController.navigate(Screen.EditEntry.createRoute(entryId))
                }
            )
        }
        composable(
            route = Screen.EditEntry.route,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) {
            EditEntryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToProfileEdit = { navController.navigate(Screen.ProfileEdit.route) }
            )
        }
    }
}
