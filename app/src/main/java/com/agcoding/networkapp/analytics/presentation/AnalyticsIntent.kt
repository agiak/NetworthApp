package com.agcoding.networkapp.analytics.presentation

sealed interface AnalyticsIntent {
    data class SelectFilter(val filter: TimeFilter) : AnalyticsIntent
    data class SelectAccount(val accountId: Long?) : AnalyticsIntent
    data object ClearError : AnalyticsIntent
}
