package com.agcoding.networkapp.goal.presentation

sealed interface GoalIntent {
    data class UpdateTargetInput(val input: String) : GoalIntent
    data class SelectTimeframe(val timeframe: GoalTimeframe) : GoalIntent
    data class UpdateCustomYears(val input: String) : GoalIntent
    data object ClearError : GoalIntent
}
