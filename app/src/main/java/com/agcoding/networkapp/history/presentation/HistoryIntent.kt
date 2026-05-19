package com.agcoding.networkapp.history.presentation

sealed interface HistoryIntent {
    data object LoadEntries : HistoryIntent
    data class SelectAccount(val accountId: Long?) : HistoryIntent
    data class UpdateSearch(val query: String) : HistoryIntent
    data class DeleteEntry(val id: Long) : HistoryIntent
}
