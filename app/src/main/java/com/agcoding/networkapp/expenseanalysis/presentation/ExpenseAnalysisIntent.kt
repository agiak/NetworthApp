package com.agcoding.networkapp.expenseanalysis.presentation

import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory

sealed interface ExpenseAnalysisIntent {
    /** null = all accounts together */
    data class SelectAccount(val accountId: Long?) : ExpenseAnalysisIntent
    data class ToggleCategory(val category: ExpenseCategory) : ExpenseAnalysisIntent
    data class EditCategory(val expenseId: Long) : ExpenseAnalysisIntent
    data class PickCategory(val category: ExpenseCategory) : ExpenseAnalysisIntent
    data object ResetToAutomatic : ExpenseAnalysisIntent
    data object DismissPicker : ExpenseAnalysisIntent
    data object NavigateToFixedExpenses : ExpenseAnalysisIntent
    data object ClearError : ExpenseAnalysisIntent
}
