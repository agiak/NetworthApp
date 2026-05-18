package com.agcoding.networkapp.compare.presentation.mapper

import com.agcoding.networkapp.compare.presentation.CompareSpeed
import com.agcoding.networkapp.compare.presentation.CompareStability
import com.agcoding.networkapp.home.presentation.model.ChartPoint

data class CompareMapResult(
    val hasData: Boolean = false,
    val currentLabel: String = "",
    val previousLabel: String = "",
    val currentTotalGrowth: String = "",
    val previousTotalGrowth: String = "",
    val currentTotalGrowthPositive: Boolean = true,
    val previousTotalGrowthPositive: Boolean = true,
    val growthDiff: String = "",
    val growthImproved: Boolean = true,
    val currentAvgMonthly: String = "",
    val previousAvgMonthly: String = "",
    val avgMonthlyDiff: String = "",
    val avgMonthlyImproved: Boolean = true,
    val avgMonthlyChangePercent: String = "",
    val speed: CompareSpeed = CompareSpeed.STABLE,
    val stability: CompareStability = CompareStability.SIMILAR,
    val stabilityCurrentVolatility: String = "",
    val stabilityPreviousVolatility: String = "",
    val bestMonthLabel: String = "",
    val bestMonthValue: String = "",
    val currentChartData: List<ChartPoint> = emptyList(),
    val previousChartData: List<ChartPoint> = emptyList(),
)
