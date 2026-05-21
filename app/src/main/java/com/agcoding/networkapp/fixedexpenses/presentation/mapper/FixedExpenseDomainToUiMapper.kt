package com.agcoding.networkapp.fixedexpenses.presentation.mapper

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.presentation.model.AccountExpenseStatsUiModel
import com.agcoding.networkapp.fixedexpenses.presentation.model.FixedExpenseUiModel
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class FixedExpenseDomainToUiMapper @Inject constructor() {

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    fun map(expense: FixedExpense, currency: AppCurrency, accounts: List<Account>) = FixedExpenseUiModel(
        id = expense.id,
        title = expense.title,
        note = expense.note,
        formattedCost = when (expense.recurrence) {
            RecurrenceType.MONTHLY -> "${currency.symbol}${fmt(expense.cost)} / mo"
            RecurrenceType.ANNUAL  -> "${currency.symbol}${fmt(expense.cost)} / yr"
        },
        costRaw = expense.cost,
        formattedDate = expense.date?.format(dateFormatter),
        recurrence = expense.recurrence,
        monthlyEquivalent = if (expense.recurrence == RecurrenceType.ANNUAL) {
            "${currency.symbol}${fmt(expense.cost / 12.0)} / mo"
        } else null,
        accountIds = expense.accountIds,
        accountColors = if (expense.accountIds.isEmpty()) emptyList()
                        else accounts.filter { it.id in expense.accountIds }.map { it.colorHex },
    )

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
    ): List<AccountExpenseStatsUiModel> = accounts.map { account ->
        val accountExpenses = expenses.filter { expense ->
            expense.accountIds.isEmpty() || account.id in expense.accountIds
        }
        val monthlyTotal = accountExpenses.sumOf { expense ->
            when (expense.recurrence) {
                RecurrenceType.MONTHLY -> expense.cost
                RecurrenceType.ANNUAL  -> expense.cost / 12.0
            }
        }
        AccountExpenseStatsUiModel(
            accountId = account.id,
            accountName = account.name,
            accountColorHex = account.colorHex,
            count = accountExpenses.size,
            formattedMonthlyTotal = "${currency.symbol}${fmt(monthlyTotal)}",
        )
    }

    private fun fmt(value: Double) = String.format(Locale.US, "%,.2f", value)
}
