package com.agcoding.networkapp.analytics.presentation.prediction

sealed interface PredictionIntent {
    data class SelectRange(val range: PredictionRange) : PredictionIntent
    data object ClearError : PredictionIntent
}
