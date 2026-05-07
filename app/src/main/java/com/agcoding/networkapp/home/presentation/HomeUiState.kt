package com.agcoding.networkapp.home.presentation

import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.home.presentation.model.InsightData
import com.agcoding.networkapp.shared.ui.model.EntryUiModel
import java.time.LocalDate

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val userInitial: String = "",
    val targetAmount: String = "",
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
    val isStreakPositive: Boolean = true,
    val currentNetWorthRaw: Double = 0.0,
    val targetAmountRaw: Double = 0.0,
    val hasGoal: Boolean = false,
    val recentEntries: List<EntryUiModel> = emptyList(),
    val isAddEntrySheetVisible: Boolean = false,
    val entryInput: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val isSaving: Boolean = false,
    val error: String? = null
)
