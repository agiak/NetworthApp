package com.agcoding.networkapp.recap.presentation

sealed interface RecapIntent {
    data class SelectYear(val year: Int) : RecapIntent
    data class SelectAccount(val accountId: Long?) : RecapIntent
    data object ClearError : RecapIntent
}
