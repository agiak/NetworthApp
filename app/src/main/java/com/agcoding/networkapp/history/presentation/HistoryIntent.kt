package com.agcoding.networkapp.history.presentation

sealed interface HistoryIntent {
    data object LoadEntries : HistoryIntent
}
