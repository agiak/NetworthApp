package com.agcoding.networkapp.savings.domain.model

import com.agcoding.networkapp.account.domain.model.Account

data class AccountSavingsPlan(
    val account: Account,
    val monthlyFixedExpenses: Double,
    val expenseCount: Int,
) {
    val monthlySalary: Double get() = account.monthlySalary
    val monthlySavings: Double get() = monthlySalary - monthlyFixedExpenses

    /** Share of the salary left after fixed expenses, or null when no salary is set. */
    val savingsRate: Double? get() = if (monthlySalary > 0.0) monthlySavings / monthlySalary else null
}

data class SavingsPlan(
    val accounts: List<AccountSavingsPlan>,
    val totalExpenseCount: Int,
) {
    val totalSalary: Double get() = accounts.sumOf { it.monthlySalary }
    val totalFixedExpenses: Double get() = accounts.sumOf { it.monthlyFixedExpenses }
    val totalSavings: Double get() = totalSalary - totalFixedExpenses
    val savingsRate: Double? get() = if (totalSalary > 0.0) totalSavings / totalSalary else null
}

data class SavingsProjection(
    val years: Int,
    val total: Double,
    val deposits: Double,
) {
    val returns: Double get() = total - deposits
}

/** Salary, fixed expenses and savings of one account or of everyone together. */
data class SavingsSelection(
    val monthlySalary: Double,
    val monthlyFixedExpenses: Double,
    /** Selected accounts with no salary set: their expenses count with no income against them. */
    val accountsWithoutSalary: List<Account>,
) {
    val monthlySavings: Double get() = monthlySalary - monthlyFixedExpenses
    val hasSalary: Boolean get() = monthlySalary > 0.0
}

/** @param accountId one account, or null for all accounts together */
fun SavingsPlan.selection(accountId: Long?): SavingsSelection {
    val selected = if (accountId == null) accounts else accounts.filter { it.account.id == accountId }
    return SavingsSelection(
        monthlySalary         = selected.sumOf { it.monthlySalary },
        monthlyFixedExpenses  = selected.sumOf { it.monthlyFixedExpenses },
        accountsWithoutSalary = selected.filter { it.monthlySalary <= 0.0 }.map { it.account },
    )
}
