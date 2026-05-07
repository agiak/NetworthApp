package com.agcoding.networkapp.analytics.presentation

import com.agcoding.networkapp.analytics.presentation.model.MonthlyEntryUiModel
import com.agcoding.networkapp.home.presentation.model.ChartPoint

enum class TimeFilter { THREE_MONTHS, SIX_MONTHS, TWELVE_MONTHS, ALL }

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val selectedFilter: TimeFilter = TimeFilter.TWELVE_MONTHS,
    val chartData: List<ChartPoint> = emptyList(),
    val chartStartLabel: String = "",
    val chartMidLabel: String = "",
    val chartEndLabel: String = "",
    val totalGrowth: String = "",
    val totalGrowthPercent: String = "",
    val totalGrowthPositive: Boolean = true,
    val avgMonthlyGrowth: String = "",
    val bestMonthLabel: String = "",
    val bestMonthValue: String = "",
    val worstMonthLabel: String = "",
    val worstMonthValue: String = "",
    val highestNetWorth: String = "",
    val highestNetWorthDate: String = "",
    val lowestNetWorth: String = "",
    val lowestNetWorthDate: String = "",
    val trendLabel: String = "",
    val trendDescription: String = "",
    val trendIsPositive: Boolean = false,
    val trendIsNeutral: Boolean = true,
    val consistencyPercent: String = "",
    val consistencyDetail: String = "",
    val projectedNetWorth: String = "",
    val projectedNetWorthDate: String = "",
    val currentStreakLabel: String = "",
    val currentStreakSubLabel: String = "",
    val monthlyEntries: List<MonthlyEntryUiModel> = emptyList(),
    val hasData: Boolean = false,
    val error: String? = null
)
