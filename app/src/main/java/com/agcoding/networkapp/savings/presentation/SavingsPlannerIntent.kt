package com.agcoding.networkapp.savings.presentation

sealed interface SavingsPlannerIntent {
    data class EditSalary(val accountId: Long) : SavingsPlannerIntent
    data class UpdateSalaryInput(val value: String) : SavingsPlannerIntent
    data object SaveSalary : SavingsPlannerIntent
    data object DismissSalaryDialog : SavingsPlannerIntent
    data object NavigateToFixedExpenses : SavingsPlannerIntent
    data object NavigateToCalculator : SavingsPlannerIntent
    data object NavigateToScenario : SavingsPlannerIntent
    data object ClearError : SavingsPlannerIntent
}
