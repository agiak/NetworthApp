package com.agcoding.networkapp.history.presentation

sealed interface HistoryIntent {
    data object LoadEntries : HistoryIntent
    data class SelectAccount(val accountId: Long?) : HistoryIntent
    data class DeleteEntry(val id: Long) : HistoryIntent
}
