package com.agcoding.networkapp.savings.presentation

import com.agcoding.networkapp.savings.presentation.model.SavingsAccountUiModel
import com.agcoding.networkapp.savings.presentation.model.SavingsSummaryUiModel

data class SavingsPlannerUiState(
    val isLoading: Boolean = true,
    val currencySymbol: String = "€",
    val accounts: List<SavingsAccountUiModel> = emptyList(),
    val summary: SavingsSummaryUiModel? = null,
    val totalExpenseCount: Int = 0,
    val editingAccount: SavingsAccountUiModel? = null,
    val salaryInput: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)
