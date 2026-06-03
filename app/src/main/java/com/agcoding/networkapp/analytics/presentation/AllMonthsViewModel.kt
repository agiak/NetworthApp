package com.agcoding.networkapp.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.account.domain.usecase.GetAccountsUseCase
import com.agcoding.networkapp.analytics.presentation.mapper.AnalyticsUiMapper
import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCase
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntriesUseCase
import com.agcoding.networkapp.home.domain.usecase.computeMonthlyForAccount
import com.agcoding.networkapp.home.domain.usecase.computeMonthlyForAccounts
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AllMonthsViewModel @Inject constructor(
    private val getMonthlyNetWorthUseCase: GetMonthlyNetWorthUseCase,
    private val getNetWorthEntriesUseCase: GetNetWorthEntriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val mapper: AnalyticsUiMapper,
) : ViewModel() {

    private val _allMonthly = MutableStateFlow<List<MonthlyNetWorth>>(emptyList())
    private val _rawEntries = MutableStateFlow<List<NetWorthEntry>>(emptyList())
    private val _uiState    = MutableStateFlow(AllMonthsUiState())
    val uiState: StateFlow<AllMonthsUiState> = _uiState.asStateFlow()

    private var currentCurrency: AppCurrency = AppCurrency.EUR

    init {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currency ->
                currentCurrency = currency
                recompute()
            }
        }
        viewModelScope.launch {
            getMonthlyNetWorthUseCase().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _allMonthly.value = data
                        recompute()
                    },
                    onFailure = { Timber.e(it); _uiState.update { s -> s.copy(isLoading = false) } }
                )
            }
        }
        viewModelScope.launch {
            getNetWorthEntriesUseCase().collect { result ->
                result.fold(onSuccess = { _rawEntries.value = it; recompute() }, onFailure = {})
            }
        }
        viewModelScope.launch {
            getAccountsUseCase().collect { accounts ->
                val validIds = _uiState.value.selectedAccountIds.filter { id -> accounts.any { it.id == id } }.toSet()
                _uiState.update { it.copy(accounts = accounts, selectedAccountIds = validIds) }
                recompute()
            }
        }
    }

    fun onIntent(intent: AllMonthsIntent) {
        when (intent) {
            is AllMonthsIntent.ToggleAccount -> {
                val current = _uiState.value.selectedAccountIds
                val updated = if (intent.accountId in current) current - intent.accountId else current + intent.accountId
                _uiState.update { it.copy(selectedAccountIds = updated) }
                recompute()
            }
            AllMonthsIntent.ClearAccountFilter -> {
                _uiState.update { it.copy(selectedAccountIds = emptySet()) }
                recompute()
            }
            is AllMonthsIntent.SelectSort -> {
                _uiState.update { it.copy(sortOrder = intent.sortOrder) }
                recompute()
            }
            is AllMonthsIntent.SelectFilter -> {
                _uiState.update { it.copy(selectedFilter = intent.filter) }
                recompute()
            }
        }
    }

    private fun recompute() {
        val state = _uiState.value
        val monthly = resolveMonthly(state.selectedAccountIds)
        if (monthly.isEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        val relevantEntries = when {
            state.selectedAccountIds.isEmpty() -> _rawEntries.value
            else -> _rawEntries.value.filter { it.accountId in state.selectedAccountIds }
        }
        val entries = mapper.map(monthly, state.selectedFilter, currentCurrency, relevantEntries).monthlyEntries
        val sorted = applySortOrder(entries, state.sortOrder)
        _uiState.update { it.copy(isLoading = false, monthlyEntries = sorted) }
    }

    private fun resolveMonthly(accountIds: Set<Long>): List<MonthlyNetWorth> = when {
        accountIds.isEmpty()  -> _allMonthly.value
        accountIds.size == 1  -> computeMonthlyForAccount(_rawEntries.value, accountIds.first())
        else                  -> computeMonthlyForAccounts(_rawEntries.value, accountIds)
    }

    private fun applySortOrder(
        entries: List<com.agcoding.networkapp.analytics.presentation.model.MonthlyEntryUiModel>,
        order: AllMonthsSortOrder,
    ) = when (order) {
        AllMonthsSortOrder.NEWEST_FIRST  -> entries
        AllMonthsSortOrder.OLDEST_FIRST  -> entries.reversed()
        AllMonthsSortOrder.HIGHEST_VALUE -> entries.sortedByDescending { it.rawValue }
        AllMonthsSortOrder.LOWEST_VALUE  -> entries.sortedBy { it.rawValue }
    }
}
