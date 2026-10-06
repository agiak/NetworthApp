package com.agcoding.networkapp.savings.domain.usecase

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.savings.domain.model.AccountSavingsPlan
import com.agcoding.networkapp.savings.domain.model.SavingsPlan

/**
 * Splits every fixed expense across the accounts that carry it, so the per-account
 * amounts always add up to the household total:
 * - no account assigned (= all) → split evenly across all accounts
 * - assigned to several accounts (e.g. shared rent) → split evenly across those accounts
 * - assigned only to accounts that no longer exist → treated as shared by all
 */
fun computeSavingsPlan(accounts: List<Account>, expenses: List<FixedExpense>): SavingsPlan {
    if (accounts.isEmpty()) return SavingsPlan(accounts = emptyList(), totalExpenseCount = expenses.size)

    val allIds = accounts.map { it.id }
    val shares = mutableMapOf<Long, Double>()
    val counts = mutableMapOf<Long, Int>()

    expenses.forEach { expense ->
        val assigned = expense.accountIds.filter { it in allIds }
        val targets = assigned.ifEmpty { allIds }
        val share = expense.monthlyCost() / targets.size
        targets.forEach { id ->
            shares[id] = (shares[id] ?: 0.0) + share
            counts[id] = (counts[id] ?: 0) + 1
        }
    }

    return SavingsPlan(
        accounts = accounts.map { account ->
            AccountSavingsPlan(
                account              = account,
                monthlyFixedExpenses = shares[account.id] ?: 0.0,
                expenseCount         = counts[account.id] ?: 0,
            )
        },
        totalExpenseCount = expenses.size,
    )
}

private fun FixedExpense.monthlyCost(): Double = when (recurrence) {
    RecurrenceType.MONTHLY -> cost
    RecurrenceType.ANNUAL  -> cost / 12.0
}
