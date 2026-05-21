package com.agcoding.networkapp.fixedexpenses.presentation

import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.presentation.model.FixedExpenseUiModel
import java.time.LocalDate

data class FixedExpensesUiState(
    val isLoading: Boolean = true,
    val expenses: List<FixedExpenseUiModel> = emptyList(),
    val totalFormatted: String = "€0.00",
    val yearlyFormatted: String = "€0.00",
    val currencySymbol: String = "€",
    val isSheetVisible: Boolean = false,
    val editingExpense: FixedExpenseUiModel? = null,
    val titleInput: String = "",
    val noteInput: String = "",
    val costInput: String = "",
    val dateInput: LocalDate? = null,
    val recurrenceInput: RecurrenceType = RecurrenceType.MONTHLY,
    val isSaving: Boolean = false,
    val error: String? = null,
)
