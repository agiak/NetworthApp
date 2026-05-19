package com.agcoding.networkapp.history.presentation

import com.agcoding.networkapp.account.domain.model.Account
import java.time.LocalDate

data class EditEntryUiState(
    val isLoading: Boolean = true,
    val amountInput: String = "",
    val noteInput: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long = 1L,
    val isSaving: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null,
)
