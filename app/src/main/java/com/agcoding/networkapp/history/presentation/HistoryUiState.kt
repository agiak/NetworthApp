package com.agcoding.networkapp.history.presentation

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.shared.ui.model.GroupedEntries

data class HistoryUiState(
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long? = null,
    val groupedEntries: List<GroupedEntries> = emptyList(),
    val error: String? = null,
)
