package com.agcoding.networkapp.shared.navigation

sealed class Screen(val route: String) {
    data object Accounts : Screen("accounts")
    data object AllMonths : Screen("all_months")
    data object Analytics : Screen("analytics")
    data object EditEntry : Screen("edit_entry/{entryId}") {
        fun createRoute(entryId: Long) = "edit_entry/$entryId"
    }
    data object EntryDetails : Screen("entry_details/{entryId}") {
        fun createRoute(entryId: Long) = "entry_details/$entryId"
    }
    data object History : Screen("history")
    data object Home : Screen("home")
    data object ProfileEdit : Screen("profile_edit")
    data object ProfileSetup : Screen("profile_setup")
    data object ProfileTargetSetup : Screen("profile_target_setup")
    data object Prediction : Screen("prediction")
    data object Settings : Screen("settings")
}
