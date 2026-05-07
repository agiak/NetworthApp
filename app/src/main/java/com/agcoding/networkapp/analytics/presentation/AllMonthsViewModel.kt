package com.agcoding.networkapp.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.analytics.presentation.mapper.AnalyticsUiMapper
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCase
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
    private val mapper: AnalyticsUiMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllMonthsUiState())
    val uiState: StateFlow<AllMonthsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMonthlyNetWorthUseCase().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        val entries = mapper.map(data, TimeFilter.ALL).monthlyEntries
                        _uiState.update { it.copy(isLoading = false, monthlyEntries = entries) }
                    },
                    onFailure = { error ->
                        Timber.e(error)
                        _uiState.update { it.copy(isLoading = false) }
                    }
                )
            }
        }
    }
}
