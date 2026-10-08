package com.agcoding.networkapp.expenseanalysis.presentation

import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory
import com.agcoding.networkapp.expenseanalysis.presentation.model.AccountFilterUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.AnalysisSummaryUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.AnalyzedExpenseUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.CategoryUiModel

data class ExpenseAnalysisUiState(
    val isLoading: Boolean = true,
    val hasAnyExpenses: Boolean = false,
    val accounts: List<AccountFilterUiModel> = emptyList(),
    /** null = all accounts together */
    val selectedAccountId: Long? = null,
    val summary: AnalysisSummaryUiModel? = null,
    val categories: List<CategoryUiModel> = emptyList(),
    val uncertainCount: Int = 0,
    val learnedChoicesCount: Int = 0,
    val expandedCategories: Set<ExpenseCategory> = emptySet(),
    /** Expense whose category is being picked. */
    val editingExpense: AnalyzedExpenseUiModel? = null,
    val error: String? = null,
)
