package com.agcoding.networkapp.shared.navigation

sealed class Screen(val route: String) {
    data object Accounts : Screen("accounts")
    data object Analytics : Screen("analytics")
    data object History : Screen("history")
    data object Home : Screen("home")
    data object ProfileEdit : Screen("profile_edit")
    data object ProfileSetup : Screen("profile_setup")
    data object ProfileTargetSetup : Screen("profile_target_setup")
    data object Settings : Screen("settings")
}
