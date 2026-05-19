package com.agcoding.networkapp.analytics.presentation

sealed interface AllMonthsIntent {
    data class SelectAccount(val accountId: Long?) : AllMonthsIntent
    data class SelectSort(val sortOrder: AllMonthsSortOrder) : AllMonthsIntent
}
