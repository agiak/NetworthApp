package com.agcoding.networkapp.compare.presentation

sealed interface CompareIntent {
    data class SelectMode(val mode: CompareMode) : CompareIntent
    data class SetCustomCurrentStart(val millis: Long) : CompareIntent
    data class SetCustomCurrentEnd(val millis: Long) : CompareIntent
    data class SetCustomPreviousStart(val millis: Long) : CompareIntent
    data class SetCustomPreviousEnd(val millis: Long) : CompareIntent
    data object ClearError : CompareIntent
}
