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
    private val _uiState   = MutableStateFlow(AllMonthsUiState())
    val uiState: StateFlow<AllMonthsUiState> = _uiState.asStateFlow()

    private var currentCurrency: AppCurrency = AppCurrency.EUR

    init {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currency ->
                currentCurrency = currency
                applyData(_allMonthly.value, _uiState.value.selectedAccountId)
            }
        }
        viewModelScope.launch {
            getMonthlyNetWorthUseCase().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _allMonthly.value = data
                        applyData(data, _uiState.value.selectedAccountId)
                    },
                    onFailure = { Timber.e(it); _uiState.update { s -> s.copy(isLoading = false) } }
                )
            }
        }
        viewModelScope.launch {
            getNetWorthEntriesUseCase().collect { result ->
                result.fold(onSuccess = { _rawEntries.value = it }, onFailure = {})
            }
        }
        viewModelScope.launch {
            getAccountsUseCase().collect { accounts ->
                val currentSel = _uiState.value.selectedAccountId
                val validSel = if (accounts.any { it.id == currentSel }) currentSel else null
                _uiState.update { it.copy(accounts = accounts, selectedAccountId = validSel) }
            }
        }
    }

    fun onIntent(intent: AllMonthsIntent) {
        when (intent) {
            is AllMonthsIntent.SelectAccount -> {
                _uiState.update { it.copy(selectedAccountId = intent.accountId) }
                val monthly = if (intent.accountId != null)
                    computeMonthlyForAccount(_rawEntries.value, intent.accountId)
                else _allMonthly.value
                applyData(monthly, intent.accountId)
            }
        }
    }

    private fun applyData(data: List<MonthlyNetWorth>, accountId: Long?) {
        val monthly = if (accountId != null) computeMonthlyForAccount(_rawEntries.value, accountId) else data
        if (monthly.isEmpty()) return
        val entries = mapper.map(monthly, TimeFilter.ALL, currentCurrency).monthlyEntries
        _uiState.update { it.copy(isLoading = false, monthlyEntries = entries) }
    }

}
