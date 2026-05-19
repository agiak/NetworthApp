package com.agcoding.networkapp.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.usecase.GetAccountsUseCase
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.usecase.DeleteNetWorthEntryUseCase
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntriesUseCase
import com.agcoding.networkapp.home.presentation.mapper.NetWorthEntryToUiMapper
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import com.agcoding.networkapp.shared.ui.model.GroupedEntries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getNetWorthEntriesUseCase: GetNetWorthEntriesUseCase,
    private val deleteNetWorthEntryUseCase: DeleteNetWorthEntryUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val mapper: NetWorthEntryToUiMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var cachedEntries: List<NetWorthEntry> = emptyList()
    private var cachedAccounts: List<Account> = emptyList()
    private var currentCurrency: AppCurrency = AppCurrency.EUR

    init {
        observeAccounts()
        observeEntries()
        observeCurrency()
    }

    fun onIntent(intent: HistoryIntent) {
        when (intent) {
            HistoryIntent.LoadEntries -> Unit
            is HistoryIntent.SelectAccount -> {
                _uiState.update { it.copy(selectedAccountId = intent.accountId) }
                applyEntries(cachedEntries, intent.accountId, _uiState.value.searchQuery)
            }
            is HistoryIntent.UpdateSearch -> {
                _uiState.update { it.copy(searchQuery = intent.query) }
                applyEntries(cachedEntries, _uiState.value.selectedAccountId, intent.query)
            }
            is HistoryIntent.SelectDateFilter -> {
                _uiState.update { it.copy(dateFilter = intent.filter) }
                applyEntries(cachedEntries, _uiState.value.selectedAccountId)
            }
            is HistoryIntent.SelectSort -> {
                _uiState.update { it.copy(sortOrder = intent.sortOrder) }
                applyEntries(cachedEntries, _uiState.value.selectedAccountId)
            }
            is HistoryIntent.RequestDeleteConfirmation -> {
                _uiState.update { it.copy(pendingDeleteId = intent.id) }
            }
            HistoryIntent.DismissDeleteConfirmation -> {
                _uiState.update { it.copy(pendingDeleteId = null) }
            }
            is HistoryIntent.DeleteEntry -> {
                _uiState.update { it.copy(pendingDeleteId = null) }
                deleteEntry(intent.id)
            }
        }
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            getAccountsUseCase().collect { accounts ->
                cachedAccounts = accounts
                val currentSel = _uiState.value.selectedAccountId
                val validSel = if (accounts.any { it.id == currentSel }) currentSel else null
                _uiState.update { it.copy(accounts = accounts, selectedAccountId = validSel) }
                applyEntries(cachedEntries, validSel)
            }
        }
    }

    private fun observeCurrency() {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currency ->
                currentCurrency = currency
                applyEntries(cachedEntries, _uiState.value.selectedAccountId, _uiState.value.searchQuery)
            }
        }
    }

    private fun observeEntries() {
        viewModelScope.launch {
            getNetWorthEntriesUseCase().collect { result ->
                result.fold(
                    onSuccess = { entries ->
                        cachedEntries = entries
                        _uiState.update { it.copy(isLoading = false) }
                        applyEntries(entries, _uiState.value.selectedAccountId)
                    },
                    onFailure = { error ->
                        Timber.e(error)
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }

    private fun applyEntries(
        entries: List<NetWorthEntry>,
        accountId: Long?,
        query: String = _uiState.value.searchQuery,
    ) {
        var filtered = if (accountId != null) entries.filter { it.accountId == accountId } else entries

        val cutoff: LocalDate? = when (_uiState.value.dateFilter) {
            HistoryDateFilter.ALL -> null
            HistoryDateFilter.ONE_MONTH -> LocalDate.now().minusMonths(1)
            HistoryDateFilter.THREE_MONTHS -> LocalDate.now().minusMonths(3)
            HistoryDateFilter.SIX_MONTHS -> LocalDate.now().minusMonths(6)
            HistoryDateFilter.ONE_YEAR -> LocalDate.now().minusYears(1)
        }
        if (cutoff != null) filtered = filtered.filter { it.date >= cutoff }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter { entry ->
                entry.note.lowercase().contains(q) ||
                entry.value.toLong().toString().contains(q) ||
                entry.date.toString().contains(q)
            }
        }

        val grouped = mapper.groupByMonth(filtered, currentCurrency)
        val sortedGroups = when (_uiState.value.sortOrder) {
            HistorySortOrder.NEWEST_FIRST -> grouped
            HistorySortOrder.OLDEST_FIRST -> grouped.reversed()
        }
        val withAccounts = enrichWithAccounts(sortedGroups, entries)
        _uiState.update { it.copy(groupedEntries = withAccounts) }
    }

    // Map accountId on each raw entry back to color/name for the UI model
    private fun enrichWithAccounts(
        grouped: List<GroupedEntries>,
        rawEntries: List<NetWorthEntry>,
    ): List<GroupedEntries> {
        val accountsById  = cachedAccounts.associateBy { it.id }
        val entryById     = rawEntries.associateBy { it.id }
        return grouped.map { group ->
            group.copy(entries = group.entries.map { ui ->
                val account = entryById[ui.id]?.let { accountsById[it.accountId] }
                ui.copy(
                    accountColorHex = account?.colorHex ?: "",
                    accountName     = account?.name ?: "",
                )
            })
        }
    }

    private fun deleteEntry(id: Long) {
        viewModelScope.launch(ioDispatcher) {
            deleteNetWorthEntryUseCase(id).onFailure { error ->
                Timber.e(error)
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }
}
