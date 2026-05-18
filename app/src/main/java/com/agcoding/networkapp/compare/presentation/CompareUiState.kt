package com.agcoding.networkapp.compare.presentation

import com.agcoding.networkapp.home.presentation.model.ChartPoint

enum class CompareSpeed { ACCELERATING, SLOWING, STABLE }
enum class CompareStability { MORE_STABLE, LESS_STABLE, SIMILAR }

data class CompareUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasData: Boolean = false,
    val selectedMode: CompareMode = CompareMode.YEAR_VS_YEAR,
    val currentLabel: String = "",
    val previousLabel: String = "",
    // Total Growth
    val currentTotalGrowth: String = "",
    val previousTotalGrowth: String = "",
    val currentTotalGrowthPositive: Boolean = true,
    val previousTotalGrowthPositive: Boolean = true,
    val growthDiff: String = "",
    val growthImproved: Boolean = true,
    // Avg Monthly
    val currentAvgMonthly: String = "",
    val previousAvgMonthly: String = "",
    val avgMonthlyDiff: String = "",
    val avgMonthlyImproved: Boolean = true,
    val avgMonthlyChangePercent: String = "",
    // Speed analysis
    val speed: CompareSpeed = CompareSpeed.STABLE,
    // Stability analysis
    val stability: CompareStability = CompareStability.SIMILAR,
    val stabilityCurrentVolatility: String = "",
    val stabilityPreviousVolatility: String = "",
    // Best month
    val bestMonthLabel: String = "",
    val bestMonthValue: String = "",
    // Chart
    val currentChartData: List<ChartPoint> = emptyList(),
    val previousChartData: List<ChartPoint> = emptyList(),
    // Custom date range (epoch millis, nullable = not set)
    val customCurrentStart: Long? = null,
    val customCurrentEnd: Long? = null,
    val customPreviousStart: Long? = null,
    val customPreviousEnd: Long? = null,
)
