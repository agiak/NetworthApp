package com.agcoding.networkapp.analytics.presentation.model

data class MonthlyEntryUiModel(
    val monthLabel: String,
    val formattedValue: String,
    val formattedDiff: String,
    val formattedPercent: String,
    val isPositive: Boolean,
    val isFirst: Boolean,
    val rawValue: Double = 0.0,
    val hasAccountAddition: Boolean = false,
    val hasAccountRemoval: Boolean = false,
    val organicDiff: Double? = null,
    val formattedOrganicDiff: String = "",
    val formattedEventAmount: String = "",   // absolute amount added or removed
    val transitionLabel: String = "",   // "Apr → May"
    val rangeLabel: String = "",        // "€10,000 → €10,420"
)
