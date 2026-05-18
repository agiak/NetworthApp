package com.agcoding.networkapp.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.analytics.presentation.mapper.AnalyticsUiMapper
import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCase
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
class AnalyticsViewModel @Inject constructor(
    private val getMonthlyNetWorthUseCase: GetMonthlyNetWorthUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val mapper: AnalyticsUiMapper
) : ViewModel() {

    private val _allData = MutableStateFlow<List<MonthlyNetWorth>>(emptyList())
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var currentCurrency: AppCurrency = AppCurrency.EUR

    init {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currency ->
                currentCurrency = currency
                recompute(_uiState.value.selectedFilter)
            }
        }
        viewModelScope.launch {
            getMonthlyNetWorthUseCase().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _allData.value = data
                        recompute(_uiState.value.selectedFilter)
                    },
                    onFailure = { error ->
                        Timber.e(error)
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }

    fun onIntent(intent: AnalyticsIntent) {
        when (intent) {
            is AnalyticsIntent.SelectFilter -> recompute(intent.filter)
            AnalyticsIntent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun recompute(filter: TimeFilter) {
        val result = mapper.map(_allData.value, filter, currentCurrency)
        _uiState.update {
            it.copy(
                isLoading = false,
                selectedFilter = filter,
                chartData = result.chartData,
                chartStartLabel = result.chartStartLabel,
                chartMidLabel = result.chartMidLabel,
                chartEndLabel = result.chartEndLabel,
                totalGrowth = result.totalGrowth,
                totalGrowthPercent = result.totalGrowthPercent,
                totalGrowthPositive = result.totalGrowthPositive,
                avgMonthlyGrowth = result.avgMonthlyGrowth,
                bestMonthLabel = result.bestMonthLabel,
                bestMonthValue = result.bestMonthValue,
                worstMonthLabel = result.worstMonthLabel,
                worstMonthValue = result.worstMonthValue,
                highestNetWorth = result.highestNetWorth,
                highestNetWorthDate = result.highestNetWorthDate,
                lowestNetWorth = result.lowestNetWorth,
                lowestNetWorthDate = result.lowestNetWorthDate,
                trendLabel = result.trendLabel,
                trendDescription = result.trendDescription,
                trendIsPositive = result.trendIsPositive,
                trendIsNeutral = result.trendIsNeutral,
                consistencyPercent = result.consistencyPercent,
                consistencyDetail = result.consistencyDetail,
                projectedNetWorth = result.projectedNetWorth,
                projectedNetWorthDate = result.projectedNetWorthDate,
                currentStreakLabel = result.currentStreakLabel,
                currentStreakSubLabel = result.currentStreakSubLabel,
                monthlyEntries = result.monthlyEntries,
                hasData = result.hasData
            )
        }
    }
}
