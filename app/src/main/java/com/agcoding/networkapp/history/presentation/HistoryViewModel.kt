package com.agcoding.networkapp.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntriesUseCase
import com.agcoding.networkapp.home.presentation.mapper.NetWorthEntryToUiMapper
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
class HistoryViewModel @Inject constructor(
    private val getNetWorthEntriesUseCase: GetNetWorthEntriesUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val mapper: NetWorthEntryToUiMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var cachedEntries: List<NetWorthEntry> = emptyList()
    private var currentCurrency: AppCurrency = AppCurrency.EUR

    init {
        loadEntries()
        observeCurrency()
    }

    private fun observeCurrency() {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currency ->
                currentCurrency = currency
                if (cachedEntries.isNotEmpty()) {
                    _uiState.update { it.copy(groupedEntries = mapper.groupByMonth(cachedEntries, currency)) }
                }
            }
        }
    }

    private fun loadEntries() {
        viewModelScope.launch {
            getNetWorthEntriesUseCase().collect { result ->
                result.fold(
                    onSuccess = { entries ->
                        cachedEntries = entries
                        _uiState.update { it.copy(isLoading = false, groupedEntries = mapper.groupByMonth(entries, currentCurrency)) }
                    },
                    onFailure = { error ->
                        Timber.e(error)
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }
}
