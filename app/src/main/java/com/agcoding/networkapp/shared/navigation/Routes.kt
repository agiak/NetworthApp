package com.agcoding.networkapp.shared.navigation

import kotlinx.serialization.Serializable

@Serializable data object AccountSetupRoute
@Serializable data class  AccountDetailRoute(val accountId: Long)
@Serializable data object AccountsRoute
@Serializable data object AddSnapshotRoute
@Serializable data object CreateAccountRoute
@Serializable data object AllMonthsRoute
@Serializable data object AnalyticsRoute
@Serializable data object CompareRoute
@Serializable data class  EditEntryRoute(val entryId: Long)
@Serializable data class  EntryDetailsRoute(val entryId: Long)
@Serializable data object FixedExpensesRoute
@Serializable data object GoalRoute
@Serializable data object HistoryRoute
@Serializable data object HomeRoute
@Serializable data class  OnboardingRoute(val fromSettings: Boolean = false)
@Serializable data object PredictionRoute
@Serializable data object ProfileEditRoute
@Serializable data object ProfileSetupRoute
@Serializable data object ProfileTargetSetupRoute
@Serializable data object RecapRoute
@Serializable data class  SecuritySetupRoute(val skipPrompt: Boolean = false)
@Serializable data object SettingsRoute
