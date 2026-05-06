package com.agcoding.networkapp.home.domain.model

import java.time.LocalDate
import java.time.YearMonth

data class MonthlyNetWorth(
    val yearMonth: YearMonth,
    val value: Double,
    val lastUpdatedDate: LocalDate,
    val isCarriedForward: Boolean = false
)
