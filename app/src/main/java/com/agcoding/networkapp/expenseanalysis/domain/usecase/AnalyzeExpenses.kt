package com.agcoding.networkapp.expenseanalysis.domain.usecase

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.expenseanalysis.domain.categorizer.ExpenseCategorizer
import com.agcoding.networkapp.expenseanalysis.domain.model.AccountAmount
import com.agcoding.networkapp.expenseanalysis.domain.model.AnalyzedExpense
import com.agcoding.networkapp.expenseanalysis.domain.model.CategoryBreakdown
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseAnalysis
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.monthlyCost
import com.agcoding.networkapp.fixedexpenses.domain.model.shareFor

/**
 * Groups [expenses] by category for one account ([selectedAccountId]) or for everyone (null).
 * For a single account, shared expenses count only with that account's share, as in
 * the Fixed Expenses filter.
 */
fun analyzeExpenses(
    expenses: List<FixedExpense>,
    accounts: List<Account>,
    selectedAccountId: Long?,
    categorizer: ExpenseCategorizer,
): ExpenseAnalysis {
    val allIds = accounts.map { it.id }
    val selectedIds = if (selectedAccountId == null) allIds else listOf(selectedAccountId)

    val analyzed = expenses.mapNotNull { expense ->
        val share = if (selectedAccountId == null) 1.0 else expense.shareFor(selectedIds, allIds)
        if (share <= 0.0) return@mapNotNull null
        AnalyzedExpense(
            expense       = expense,
            prediction    = categorizer.categorize(expense.title, expense.note),
            share         = share,
            monthlyAmount = expense.monthlyCost() * share,
        )
    }

    val monthlyTotal = analyzed.sumOf { it.monthlyAmount }
    val categories = analyzed
        .groupBy { it.prediction.category }
        .map { (category, items) ->
            val categoryTotal = items.sumOf { it.monthlyAmount }
            CategoryBreakdown(
                category     = category,
                monthlyTotal = categoryTotal,
                shareOfTotal = if (monthlyTotal > 0.0) categoryTotal / monthlyTotal else 0.0,
                expenses     = items.sortedByDescending { it.monthlyAmount },
                perAccount   = accounts.map { account ->
                    AccountAmount(
                        accountId     = account.id,
                        monthlyAmount = items.sumOf { it.expense.monthlyCost() * it.expense.shareFor(listOf(account.id), allIds) },
                    )
                },
            )
        }
        .sortedByDescending { it.monthlyTotal }

    return ExpenseAnalysis(
        monthlyTotal   = monthlyTotal,
        categories     = categories,
        uncertainCount = analyzed.count { it.prediction.isUncertain },
    )
}
