package com.agcoding.networkapp.goal.presentation

import com.agcoding.networkapp.home.presentation.model.ChartPoint

data class GoalUiState(
    val isLoading: Boolean = true,
    val hasHistoricalData: Boolean = false,
    val targetInput: String = "",
    val selectedTimeframe: GoalTimeframe = GoalTimeframe.THREE_YEARS,
    val customYearsInput: String = "5",
    val currentNetWorthFormatted: String = "",
    val currentNetWorthRaw: Double = 0.0,
    val targetFormatted: String = "",
    val progressFraction: Float = 0f,
    val progressPercent: String = "",
    val status: GoalStatus = GoalStatus.ON_TRACK,
    val requiredMonthly: String = "",
    val requiredYearly: String = "",
    val avgMonthlyGrowth: String = "",
    val fasterByPercent: Int = 0,
    val yearsAtCurrentPace: String = "",
    val chartCurrentTrajectory: List<ChartPoint> = emptyList(),
    val chartRequiredTrajectory: List<ChartPoint> = emptyList(),
    val chartEndLabel: String = "",
    val isInputValid: Boolean = false,
    val error: String? = null
)
