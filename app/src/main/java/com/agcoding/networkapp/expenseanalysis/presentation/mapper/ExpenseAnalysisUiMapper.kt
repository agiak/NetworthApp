package com.agcoding.networkapp.expenseanalysis.presentation.mapper

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.expenseanalysis.domain.model.AnalyzedExpense
import com.agcoding.networkapp.expenseanalysis.domain.model.CategoryBreakdown
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseAnalysis
import com.agcoding.networkapp.expenseanalysis.presentation.model.AccountAmountUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.AnalysisSummaryUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.AnalyzedExpenseUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.CategoryUiModel
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

class ExpenseAnalysisUiMapper @Inject constructor() {

    fun mapSummary(analysis: ExpenseAnalysis, currency: AppCurrency) = AnalysisSummaryUiModel(
        formattedMonthly = money(analysis.monthlyTotal, currency),
        formattedYearly  = money(analysis.yearlyTotal, currency),
        expenseCount     = analysis.expenseCount,
        slices           = analysis.categories
            .filter { it.monthlyTotal > 0.0 }
            .map { it.category to it.shareOfTotal.toFloat() },
    )

    fun mapCategory(
        breakdown: CategoryBreakdown,
        currency: AppCurrency,
        accounts: List<Account>,
        showPerAccount: Boolean,
    ) = CategoryUiModel(
        category         = breakdown.category,
        formattedMonthly = money(breakdown.monthlyTotal, currency),
        percent          = (breakdown.shareOfTotal * 100.0).roundToInt(),
        fraction         = breakdown.shareOfTotal.toFloat(),
        expenses         = breakdown.expenses.map { mapExpense(it, currency) },
        perAccount       = if (!showPerAccount) emptyList() else breakdown.perAccount
            .filter { it.monthlyAmount > 0.0 }
            .mapNotNull { amount ->
                val account = accounts.find { it.id == amount.accountId } ?: return@mapNotNull null
                AccountAmountUiModel(account.name, account.colorHex, money(amount.monthlyAmount, currency))
            },
    )

    private fun mapExpense(item: AnalyzedExpense, currency: AppCurrency) = AnalyzedExpenseUiModel(
        id               = item.expense.id,
        title            = item.expense.title,
        formattedMonthly = money(item.monthlyAmount, currency),
        sharePercent     = if (item.share < 1.0) (item.share * 100.0).roundToInt() else null,
        category         = item.prediction.category,
        source           = item.prediction.source,
        isUncertain      = item.prediction.isUncertain,
    )

    private fun money(value: Double, currency: AppCurrency) =
        "${currency.symbol}${String.format(Locale.US, "%,.2f", value)}"
}
