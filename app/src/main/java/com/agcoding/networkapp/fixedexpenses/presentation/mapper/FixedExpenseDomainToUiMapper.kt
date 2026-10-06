package com.agcoding.networkapp.fixedexpenses.presentation.mapper

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.domain.model.monthlyCost
import com.agcoding.networkapp.fixedexpenses.domain.model.shareFor
import com.agcoding.networkapp.fixedexpenses.presentation.model.AccountExpenseStatsUiModel
import com.agcoding.networkapp.fixedexpenses.presentation.model.FixedExpenseUiModel
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

class FixedExpenseDomainToUiMapper @Inject constructor() {

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    /**
     * @param share fraction of the expense carried by the filtered accounts; below 1.0 the
     * item shows only that share and mentions the full cost.
     */
    fun map(
        expense: FixedExpense,
        currency: AppCurrency,
        accounts: List<Account>,
        share: Double = 1.0,
    ): FixedExpenseUiModel {
        val isPartial = share < 1.0
        val shownCost = expense.cost * share
        return FixedExpenseUiModel(
            id = expense.id,
            title = expense.title,
            note = expense.note,
            formattedCost = withPeriod(shownCost, expense.recurrence, currency),
            costRaw = expense.cost,
            formattedDate = expense.date?.format(dateFormatter),
            recurrence = expense.recurrence,
            monthlyEquivalent = if (expense.recurrence == RecurrenceType.ANNUAL) {
                "${currency.symbol}${fmt(shownCost / 12.0)} / mo"
            } else null,
            accountIds = expense.accountIds,
            accountColors = if (expense.accountIds.isEmpty()) emptyList()
                            else accounts.filter { it.id in expense.accountIds }.map { it.colorHex },
            sharePercent = if (isPartial) (share * 100.0).roundToInt() else null,
            formattedFullCost = if (isPartial) withPeriod(expense.cost, expense.recurrence, currency) else null,
        )
    }

    private fun withPeriod(value: Double, recurrence: RecurrenceType, currency: AppCurrency) = when (recurrence) {
        RecurrenceType.MONTHLY -> "${currency.symbol}${fmt(value)} / mo"
        RecurrenceType.ANNUAL  -> "${currency.symbol}${fmt(value)} / yr"
    }

    fun formatMonthlyTotal(expenses: List<FixedExpense>, currency: AppCurrency): String {
        val total = expenses.sumOf { expense ->
            when (expense.recurrence) {
                RecurrenceType.MONTHLY -> expense.cost
                RecurrenceType.ANNUAL  -> expense.cost / 12.0
            }
        }
        return "${currency.symbol}${fmt(total)}"
    }

    fun formatYearlyTotal(expenses: List<FixedExpense>, currency: AppCurrency): String {
        val total = expenses.sumOf { expense ->
            when (expense.recurrence) {
                RecurrenceType.MONTHLY -> expense.cost * 12.0
                RecurrenceType.ANNUAL  -> expense.cost
            }
        }
        return "${currency.symbol}${fmt(total)}"
    }

    fun computeAccountStats(
        expenses: List<FixedExpense>,
        accounts: List<Account>,
        currency: AppCurrency,
    ): List<AccountExpenseStatsUiModel> {
        if (accounts.isEmpty()) return emptyList()

        // Shared and unassigned expenses are split evenly between the accounts that carry them,
        // so the per-account totals always add up to the grand total (no double-counting).
        val allIds = accounts.map { it.id }

        return accounts.map { account ->
            val accountShares = expenses
                .map { expense -> expense to expense.shareFor(listOf(account.id), allIds) }
                .filter { (_, share) -> share > 0.0 }
            val monthlyTotal = accountShares.sumOf { (expense, share) -> expense.monthlyCost() * share }
            AccountExpenseStatsUiModel(
                accountId             = account.id,
                accountName           = account.name,
                accountColorHex       = account.colorHex,
                count                 = accountShares.size,
                formattedMonthlyTotal = "${currency.symbol}${fmt(monthlyTotal)}",
                formattedYearlyTotal  = "${currency.symbol}${fmt(monthlyTotal * 12.0)}",
            )
        }
    }

    private fun fmt(value: Double) = String.format(Locale.US, "%,.2f", value)
}
