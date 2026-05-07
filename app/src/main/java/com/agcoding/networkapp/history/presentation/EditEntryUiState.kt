package com.agcoding.networkapp.history.presentation

import java.time.LocalDate

data class EditEntryUiState(
    val isLoading: Boolean = true,
    val amountInput: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val isSaving: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
)
