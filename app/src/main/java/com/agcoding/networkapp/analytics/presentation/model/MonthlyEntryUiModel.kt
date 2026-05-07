package com.agcoding.networkapp.analytics.presentation.model

data class MonthlyEntryUiModel(
    val monthLabel: String,
    val formattedValue: String,
    val formattedDiff: String,
    val formattedPercent: String,
    val isPositive: Boolean,
    val isFirst: Boolean
)
