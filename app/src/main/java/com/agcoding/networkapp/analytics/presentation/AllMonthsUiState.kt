package com.agcoding.networkapp.analytics.presentation

import com.agcoding.networkapp.analytics.presentation.model.MonthlyEntryUiModel

data class AllMonthsUiState(
    val isLoading: Boolean = true,
    val monthlyEntries: List<MonthlyEntryUiModel> = emptyList()
)
