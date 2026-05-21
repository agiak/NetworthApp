package com.agcoding.networkapp.fixedexpenses.presentation.mapper

import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.presentation.model.FixedExpenseUiModel
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class FixedExpenseDomainToUiMapper @Inject constructor() {

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    fun map(expense: FixedExpense, currency: AppCurrency) = FixedExpenseUiModel(
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

    private fun fmt(value: Double) = String.format(Locale.US, "%,.2f", value)
}
