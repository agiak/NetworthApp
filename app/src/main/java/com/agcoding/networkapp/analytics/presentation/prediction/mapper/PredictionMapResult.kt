package com.agcoding.networkapp.analytics.presentation.prediction.mapper

import com.agcoding.networkapp.home.presentation.model.ChartPoint

data class PredictionMapResult(
    val hasData: Boolean = false,
    val expectedValue: String = "",
    val minimumValue: String = "",
    val maximumValue: String = "",
    val avgMonthlyGrowth: String = "",
    val conservativeMonthlyRate: String = "",
    val optimisticMonthlyRate: String = "",
    val chartExpected: List<ChartPoint> = emptyList(),
    val chartMinimum: List<ChartPoint> = emptyList(),
    val chartMaximum: List<ChartPoint> = emptyList(),
    val chartExpectedLabel: String = "",
    val chartMinimumLabel: String = "",
    val chartMaximumLabel: String = "",
    val chartMidLabel: String = "",
    val chartEndLabel: String = ""
)
