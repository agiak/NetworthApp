package com.agcoding.networkapp.savings.presentation.mapper

import com.agcoding.networkapp.savings.domain.model.AccountSavingsPlan
import com.agcoding.networkapp.savings.domain.model.SavingsPlan
import com.agcoding.networkapp.savings.presentation.model.SavingsAccountUiModel
import com.agcoding.networkapp.savings.presentation.model.SavingsSummaryUiModel
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

class SavingsPlanUiMapper @Inject constructor() {

    fun mapAccount(plan: AccountSavingsPlan, currency: AppCurrency) = SavingsAccountUiModel(
        accountId              = plan.account.id,
        accountName            = plan.account.name,
        accountColorHex        = plan.account.colorHex,
        salaryRaw              = plan.monthlySalary,
        hasSalary              = plan.monthlySalary > 0.0,
        expenseCount           = plan.expenseCount,
        formattedSalary        = money(plan.monthlySalary, currency),
        formattedFixedExpenses = money(plan.monthlyFixedExpenses, currency),
        formattedSavings       = money(plan.monthlySavings, currency),
        formattedYearlySavings = money(plan.monthlySavings * 12.0, currency),
        savingsRatePercent     = plan.savingsRate?.let(::percent),
        savingsShare           = share(plan.monthlySavings, plan.monthlySalary),
        isDeficit              = plan.monthlySavings < 0.0,
    )

    fun mapSummary(plan: SavingsPlan, currency: AppCurrency) = SavingsSummaryUiModel(
        formattedSalary        = money(plan.totalSalary, currency),
        formattedFixedExpenses = money(plan.totalFixedExpenses, currency),
        formattedSavings       = money(plan.totalSavings, currency),
        formattedYearlySavings = money(plan.totalSavings * 12.0, currency),
        savingsRatePercent     = plan.savingsRate?.let(::percent),
        savingsShare           = share(plan.totalSavings, plan.totalSalary),
        isDeficit              = plan.totalSavings < 0.0,
        hasSalary              = plan.totalSalary > 0.0,
    )

    private fun percent(rate: Double): Int = (rate * 100.0).roundToInt()

    private fun share(part: Double, salary: Double): Float =
        if (salary <= 0.0) 0f else (part / salary).toFloat().coerceIn(0f, 1f)

    private fun money(value: Double, currency: AppCurrency): String {
        val sign = if (value < 0.0) "-" else ""
        return "$sign${currency.symbol}${String.format(Locale.US, "%,.2f", abs(value))}"
    }
}
