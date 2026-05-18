package com.agcoding.networkapp.recap.presentation

import com.agcoding.networkapp.home.presentation.model.ChartPoint

data class MonthlyBreakdownItem(
    val monthLabel: String,
    val formattedValue: String,
    val formattedChange: String,
    val isPositive: Boolean,
    val isFirst: Boolean
)

data class RecapUiState(
    val isLoading: Boolean = true,
    val hasData: Boolean = false,
    val availableYears: List<Int> = emptyList(),
    val selectedYear: Int = java.time.LocalDate.now().year,
    // Header
    val totalGrowthFormatted: String = "",
    val totalGrowthPercent: String = "",
    val totalGrowthPositive: Boolean = true,
    val startValue: String = "",
    val endValue: String = "",
    val monthsTracked: Int = 0,
    // Highlights
    val bestMonthLabel: String = "",
    val bestMonthValue: String = "",
    val worstMonthLabel: String = "",
    val worstMonthValue: String = "",
    val avgMonthlyGrowth: String = "",
    val biggestJump: String = "",
    // Goal
    val hasGoal: Boolean = false,
    val goalProgress: Float = 0f,
    val goalProgressPercent: String = "",
    val goalYearContribution: String = "",
    // Trend
    val trend: RecapTrend = RecapTrend.STABLE,
    val isNewAllTimeHigh: Boolean = false,
    // Chart
    val chartData: List<ChartPoint> = emptyList(),
    val chartStartLabel: String = "",
    val chartEndLabel: String = "",
    // Monthly breakdown
    val monthlyBreakdown: List<MonthlyBreakdownItem> = emptyList(),
    val error: String? = null
)
