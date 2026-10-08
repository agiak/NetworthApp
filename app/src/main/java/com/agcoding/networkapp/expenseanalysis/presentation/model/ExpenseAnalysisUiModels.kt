package com.agcoding.networkapp.expenseanalysis.presentation.model

import com.agcoding.networkapp.expenseanalysis.domain.model.CategorySource
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory

data class AnalysisSummaryUiModel(
    val formattedMonthly: String,
    val formattedYearly: String,
    val expenseCount: Int,
    /** Category and its fraction of the total, largest first, for the chart. */
    val slices: List<Pair<ExpenseCategory, Float>>,
)

data class CategoryUiModel(
    val category: ExpenseCategory,
    val formattedMonthly: String,
    val percent: Int,
    val fraction: Float,
    val expenses: List<AnalyzedExpenseUiModel>,
    /** What each account pays here; only filled in the all-accounts view. */
    val perAccount: List<AccountAmountUiModel>,
)

data class AnalyzedExpenseUiModel(
    val id: Long,
    val title: String,
    val formattedMonthly: String,
    /** Set when only part of a shared expense counts for the selected account. */
    val sharePercent: Int?,
    val category: ExpenseCategory,
    val source: CategorySource,
    val isUncertain: Boolean,
)

data class AccountAmountUiModel(
    val accountName: String,
    val colorHex: String,
    val formattedMonthly: String,
)

data class AccountFilterUiModel(
    val id: Long,
    val name: String,
    val colorHex: String,
)
