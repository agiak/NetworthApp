package com.agcoding.networkapp.recap.presentation.mapper

import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.recap.presentation.MonthlyBreakdownItem
import com.agcoding.networkapp.recap.presentation.RecapTrend

data class RecapMapResult(
    val hasData: Boolean = false,
    val startValue: String = "",
    val endValue: String = "",
    val totalGrowthFormatted: String = "",
    val totalGrowthPercent: String = "",
    val totalGrowthPositive: Boolean = true,
    val bestMonthLabel: String = "",
    val bestMonthValue: String = "",
    val worstMonthLabel: String = "",
    val worstMonthValue: String = "",
    val avgMonthlyGrowth: String = "",
    val biggestJump: String = "",
    val hasGoal: Boolean = false,
    val goalProgress: Float = 0f,
    val goalProgressPercent: String = "",
    val goalYearContribution: String = "",
    val trend: RecapTrend = RecapTrend.STABLE,
    val isNewAllTimeHigh: Boolean = false,
    val monthsTracked: Int = 0,
    val chartData: List<ChartPoint> = emptyList(),
    val chartStartLabel: String = "",
    val chartEndLabel: String = "",
    val monthlyBreakdown: List<MonthlyBreakdownItem> = emptyList()
)
