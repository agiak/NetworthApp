package com.agcoding.networkapp.home.presentation.model

import java.time.YearMonth

sealed class InsightData {
    data class GrowthStreak(val streakMonths: Int) : InsightData()
    data class BestMonth(val yearMonth: YearMonth, val growthAmount: Double) : InsightData()
    data class Forecast(val targetAmount: Double, val targetYearMonth: YearMonth) : InsightData()
}
