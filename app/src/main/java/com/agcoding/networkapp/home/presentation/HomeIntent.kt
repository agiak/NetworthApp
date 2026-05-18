package com.agcoding.networkapp.home.presentation

import java.time.LocalDate

sealed interface HomeIntent {
    data object ClearError : HomeIntent
    data object HideAddEntrySheet : HomeIntent
    data object LoadData : HomeIntent
    data object SaveEntry : HomeIntent
    data object ShowAddEntrySheet : HomeIntent
    data class UpdateEntryDate(val date: LocalDate) : HomeIntent
    data class UpdateEntryInput(val value: String) : HomeIntent
    data class UpdateEntryNote(val value: String) : HomeIntent
}
