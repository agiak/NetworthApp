package com.agcoding.networkapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agcoding.networkapp.navigation.NavGraph
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.shared.navigation.Screen
import com.agcoding.networkapp.shared.ui.components.BottomNavigationBar
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun NetWorthApp(appViewModel: AppViewModel = hiltViewModel()) {
    val appTheme by appViewModel.appTheme.collectAsStateWithLifecycle()
    val isProfileCreated by appViewModel.isProfileCreated.collectAsStateWithLifecycle()

    val isDarkTheme = when (appTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    if (isProfileCreated == null) return

    NetWorthTheme(darkTheme = isDarkTheme) {
        val navController = rememberNavController()
        val currentBackStack by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStack?.destination?.route
        val showBottomBar = currentRoute != Screen.AllMonths.route &&
                           currentRoute != Screen.EditEntry.route &&
                           currentRoute != Screen.EntryDetails.route &&
                           currentRoute != Screen.Goal.route &&
                           currentRoute != Screen.History.route &&
                           currentRoute != Screen.Prediction.route &&
                           currentRoute != Screen.ProfileEdit.route &&
                           currentRoute != Screen.ProfileSetup.route &&
                           currentRoute != Screen.ProfileTargetSetup.route

        Scaffold(
            bottomBar = {
                if (showBottomBar) BottomNavigationBar(navController = navController)
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavGraph(
                    navController = navController,
                    isProfileCreated = isProfileCreated!!
                )
            }
        }
    }
}
