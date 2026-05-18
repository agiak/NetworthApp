package com.agcoding.networkapp.history.presentation

data class EntryDetailsUiState(
    val isLoading: Boolean = true,
    val entryId: Long = 0L,
    val formattedAmount: String = "",
    val formattedDate: String = "",
    val note: String = "",
    val isDeleted: Boolean = false,
    val error: String? = null
)
