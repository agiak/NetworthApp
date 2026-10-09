package com.agcoding.networkapp.savings.presentation.calculator

/** Where the monthly amount comes from. */
enum class SavingsSource {
    /** Salaries minus fixed expenses, from the Savings Planner. */
    PLAN,
    /** Typed by the user. */
    MANUAL,
}

data class SavingsProjectionUiModel(
    val years: Int,
    val formattedTotal: String,
    val formattedDeposits: String,
    /** Null when no annual return is set */
    val formattedReturns: String?,
    val isCustom: Boolean = false,
)

data class CalculatorAccountUiModel(
    val id: Long,
    val name: String,
)

/** Salaries − fixed expenses = savings, for the selected account(s). */
data class PlanBreakdownUiModel(
    val formattedSalary: String,
    val formattedFixedExpenses: String,
    val formattedSavings: String,
    val hasSalary: Boolean,
    val isDeficit: Boolean,
    /** Names of selected accounts with no salary set. */
    val accountsWithoutSalary: List<String>,
)

data class SavingsCalculatorUiState(
    val currencySymbol: String = "€",
    val source: SavingsSource = SavingsSource.PLAN,
    val accounts: List<CalculatorAccountUiModel> = emptyList(),
    /** null = all accounts together */
    val selectedAccountId: Long? = null,
    val planBreakdown: PlanBreakdownUiModel? = null,
    val monthlyAmountInput: String = "",
    val annualReturnInput: String = "",
    val customYearsInput: String = "",
    val projections: List<SavingsProjectionUiModel> = emptyList(),
    val customProjection: SavingsProjectionUiModel? = null,
)
