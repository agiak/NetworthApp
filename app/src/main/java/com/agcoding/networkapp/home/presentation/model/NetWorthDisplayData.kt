package com.agcoding.networkapp.home.presentation.model

data class NetWorthDisplayData(
    val currentNetWorth: String = "€0",
    val changeThisMonth: String = "",
    val changePercentage: String = "",
    val isPositiveChange: Boolean = true,
    val lastUpdatedDate: String = "",
    val chartData: List<ChartPoint> = emptyList(),
    val insights: List<InsightData> = emptyList(),
    val ytdGrowth: String = "€0",
    val ytdPercentage: String = "0%",
    val avgPerMonth: String = "€0",
    val streakMonths: Int = 0,
    val isStreakPositive: Boolean = true
)
