package com.agcoding.networkapp.savings.presentation.calculator

sealed interface SavingsCalculatorIntent {
    data class SelectSource(val source: SavingsSource) : SavingsCalculatorIntent
    /** null = all accounts together */
    data class SelectAccount(val accountId: Long?) : SavingsCalculatorIntent
    data class UpdateMonthlyAmount(val value: String) : SavingsCalculatorIntent
    data class UpdateAnnualReturn(val value: String) : SavingsCalculatorIntent
    data class UpdateCustomYears(val value: String) : SavingsCalculatorIntent
    data object NavigateToSavingsPlanner : SavingsCalculatorIntent
}
