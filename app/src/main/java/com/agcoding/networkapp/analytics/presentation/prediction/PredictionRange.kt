package com.agcoding.networkapp.analytics.presentation.prediction

enum class PredictionRange(val years: Int) {
    TWO_YEARS(2),
    THREE_YEARS(3),
    FIVE_YEARS(5),
    TEN_YEARS(10),
    FIFTEEN_YEARS(15);

    val months: Int get() = years * 12
}
