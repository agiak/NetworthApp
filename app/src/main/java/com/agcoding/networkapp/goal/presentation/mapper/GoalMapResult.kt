package com.agcoding.networkapp.goal.presentation.mapper

import com.agcoding.networkapp.goal.presentation.GoalStatus
import com.agcoding.networkapp.home.presentation.model.ChartPoint

data class GoalMapResult(
    val hasData: Boolean = false,
    val currentNetWorthFormatted: String = "",
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
    val chartEndLabel: String = ""
)
