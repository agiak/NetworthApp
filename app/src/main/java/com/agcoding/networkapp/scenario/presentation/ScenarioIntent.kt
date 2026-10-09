package com.agcoding.networkapp.scenario.presentation

import com.agcoding.networkapp.scenario.domain.model.HomeExpenseType

sealed interface ScenarioIntent {
    data class ToggleMove(val enabled: Boolean) : ScenarioIntent
    data class UpdateHomeCost(val type: HomeExpenseType, val value: String) : ScenarioIntent
    data class UpdateMoveOneOff(val value: String) : ScenarioIntent

    data class ToggleCars(val enabled: Boolean) : ScenarioIntent
    data class SetCurrentCars(val count: Int) : ScenarioIntent
    data class SetNewCars(val count: Int) : ScenarioIntent
    data class SetIncreasePercent(val percent: Int) : ScenarioIntent
    data class UpdateCarLine(val expenseId: Long, val value: String) : ScenarioIntent
    data class UpdateNewCarMonthly(val value: String) : ScenarioIntent
    data class UpdateCarPayment(val value: String) : ScenarioIntent
    data class UpdateCarOneOff(val value: String) : ScenarioIntent

    /** null = new expenses are shared by everyone */
    data class SelectPayer(val accountId: Long?) : ScenarioIntent
    data object Reset : ScenarioIntent
    data object NavigateToSavingsPlanner : ScenarioIntent
}
