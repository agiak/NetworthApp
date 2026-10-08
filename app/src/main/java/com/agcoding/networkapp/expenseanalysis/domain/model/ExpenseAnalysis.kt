package com.agcoding.networkapp.expenseanalysis.domain.model

import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense

data class AnalyzedExpense(
    val expense: FixedExpense,
    val prediction: CategoryPrediction,
    /** Fraction of the expense carried by the selected account(s). */
    val share: Double,
    /** Monthly amount after applying [share] (annual expenses count as a twelfth). */
    val monthlyAmount: Double,
)

data class AccountAmount(
    val accountId: Long,
    val monthlyAmount: Double,
)

data class CategoryBreakdown(
    val category: ExpenseCategory,
    val monthlyTotal: Double,
    /** Fraction of the analysed total, 0.0–1.0. */
    val shareOfTotal: Double,
    val expenses: List<AnalyzedExpense>,
    /** What each account pays in this category, regardless of the selection. */
    val perAccount: List<AccountAmount>,
)

data class ExpenseAnalysis(
    val monthlyTotal: Double,
    val categories: List<CategoryBreakdown>,
    val uncertainCount: Int,
) {
    val yearlyTotal: Double get() = monthlyTotal * 12.0
    val expenseCount: Int get() = categories.sumOf { it.expenses.size }
}
