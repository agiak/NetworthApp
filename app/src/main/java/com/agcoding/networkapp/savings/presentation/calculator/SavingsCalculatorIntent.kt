package com.agcoding.networkapp.savings.presentation.calculator

sealed interface SavingsCalculatorIntent {
    data class UpdateMonthlyAmount(val value: String) : SavingsCalculatorIntent
    data class UpdateAnnualReturn(val value: String) : SavingsCalculatorIntent
    data class UpdateCustomYears(val value: String) : SavingsCalculatorIntent
}
