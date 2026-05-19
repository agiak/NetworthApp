package com.agcoding.networkapp.history.presentation

sealed interface HistoryIntent {
    data object LoadEntries : HistoryIntent
    data class SelectAccount(val accountId: Long?) : HistoryIntent
    data class UpdateSearch(val query: String) : HistoryIntent
    data class SelectDateFilter(val filter: HistoryDateFilter) : HistoryIntent
    data class SelectSort(val sortOrder: HistorySortOrder) : HistoryIntent
    data class RequestDeleteConfirmation(val id: Long) : HistoryIntent
    data object DismissDeleteConfirmation : HistoryIntent
    data class DeleteEntry(val id: Long) : HistoryIntent
}
