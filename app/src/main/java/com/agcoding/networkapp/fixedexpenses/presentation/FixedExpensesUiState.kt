package com.agcoding.networkapp.fixedexpenses.presentation

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpenseSortOption
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.presentation.model.AccountExpenseStatsUiModel
import com.agcoding.networkapp.fixedexpenses.presentation.model.FixedExpenseUiModel
import java.time.LocalDate

data class FixedExpensesUiState(
    val isLoading: Boolean = true,
    val expenses: List<FixedExpenseUiModel> = emptyList(),
    val totalFormatted: String = "€0.00",
    val yearlyFormatted: String = "€0.00",
    val currencySymbol: String = "€",
    val sortOption: FixedExpenseSortOption = FixedExpenseSortOption.COST_HIGH,
    val filterAccountIds: Set<Long> = emptySet(),
    val availableAccounts: List<Account> = emptyList(),
    val accountStats: List<AccountExpenseStatsUiModel> = emptyList(),
    val isSheetVisible: Boolean = false,
    val editingExpense: FixedExpenseUiModel? = null,
    val titleInput: String = "",
    val noteInput: String = "",
    val costInput: String = "",
    val dateInput: LocalDate? = null,
    val recurrenceInput: RecurrenceType = RecurrenceType.MONTHLY,
    val selectedAccountIds: List<Long> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
)
