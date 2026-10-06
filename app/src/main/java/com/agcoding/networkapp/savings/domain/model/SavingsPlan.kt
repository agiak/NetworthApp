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
