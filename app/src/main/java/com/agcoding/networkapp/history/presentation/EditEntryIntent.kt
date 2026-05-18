package com.agcoding.networkapp.history.presentation

import java.time.LocalDate

sealed interface EditEntryIntent {
    data class UpdateAmount(val value: String) : EditEntryIntent
    data class UpdateDate(val date: LocalDate) : EditEntryIntent
    data class UpdateNote(val value: String) : EditEntryIntent
    data object Save : EditEntryIntent
    data object ClearError : EditEntryIntent
}
