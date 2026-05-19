package com.agcoding.networkapp.analytics.presentation

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.analytics.presentation.model.MonthlyEntryUiModel

data class AllMonthsUiState(
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long? = null,
    val monthlyEntries: List<MonthlyEntryUiModel> = emptyList(),
    val sortOrder: AllMonthsSortOrder = AllMonthsSortOrder.NEWEST_FIRST,
)
