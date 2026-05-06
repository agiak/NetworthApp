package com.agcoding.networkapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.agcoding.networkapp.history.presentation.HistoryScreen
import com.agcoding.networkapp.home.presentation.HomeScreen
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
                onNavigateToProfileEdit = { navController.navigate(Screen.ProfileEdit.route) }
            )
        }
        composable(Screen.Analytics.route) { /* TODO: Analytics feature */ }
        composable(Screen.Accounts.route) { /* TODO: Accounts feature */ }
        composable(Screen.History.route) {
            HistoryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToProfileEdit = { navController.navigate(Screen.ProfileEdit.route) }
            )
        }
    }
}
