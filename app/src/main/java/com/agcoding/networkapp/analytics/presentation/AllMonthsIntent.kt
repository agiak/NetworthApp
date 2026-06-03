package com.agcoding.networkapp.analytics.presentation

sealed interface AllMonthsIntent {
    data class ToggleAccount(val accountId: Long) : AllMonthsIntent
    data object ClearAccountFilter : AllMonthsIntent
    data class SelectSort(val sortOrder: AllMonthsSortOrder) : AllMonthsIntent
    data class SelectFilter(val filter: TimeFilter) : AllMonthsIntent
}
