package com.agcoding.networkapp.history.presentation

import com.agcoding.networkapp.shared.ui.model.GroupedEntries

data class HistoryUiState(
    val isLoading: Boolean = true,
    val groupedEntries: List<GroupedEntries> = emptyList(),
    val error: String? = null
)
