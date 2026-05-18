package com.agcoding.networkapp.compare.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.compare.presentation.mapper.CompareUiMapper
import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
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
class CompareViewModel @Inject constructor(
    private val getMonthlyNetWorthUseCase: GetMonthlyNetWorthUseCase,
    private val mapper: CompareUiMapper
) : ViewModel() {

    private val _allData = MutableStateFlow<List<MonthlyNetWorth>>(emptyList())
    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMonthlyNetWorthUseCase().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _allData.value = data
                        recompute()
                    },
                    onFailure = { error ->
                        Timber.e(error)
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }

    fun onIntent(intent: CompareIntent) {
        when (intent) {
            is CompareIntent.SelectMode -> {
                _uiState.update { it.copy(selectedMode = intent.mode) }
                recompute()
            }
            is CompareIntent.SetCustomCurrentStart -> {
                _uiState.update { it.copy(customCurrentStart = intent.millis) }
                recompute()
            }
            is CompareIntent.SetCustomCurrentEnd -> {
                _uiState.update { it.copy(customCurrentEnd = intent.millis) }
                recompute()
            }
            is CompareIntent.SetCustomPreviousStart -> {
                _uiState.update { it.copy(customPreviousStart = intent.millis) }
                recompute()
            }
            is CompareIntent.SetCustomPreviousEnd -> {
                _uiState.update { it.copy(customPreviousEnd = intent.millis) }
                recompute()
            }
            CompareIntent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun recompute() {
        val s = _uiState.value
        val mapped = mapper.map(
            allData = _allData.value,
            mode = s.selectedMode,
            customCurrentStart = s.customCurrentStart,
            customCurrentEnd = s.customCurrentEnd,
            customPreviousStart = s.customPreviousStart,
            customPreviousEnd = s.customPreviousEnd
        )
        _uiState.update {
            it.copy(
                isLoading = false,
                hasData = mapped.hasData,
                currentLabel = mapped.currentLabel,
                previousLabel = mapped.previousLabel,
                currentTotalGrowth = mapped.currentTotalGrowth,
                previousTotalGrowth = mapped.previousTotalGrowth,
                currentTotalGrowthPositive = mapped.currentTotalGrowthPositive,
                previousTotalGrowthPositive = mapped.previousTotalGrowthPositive,
                growthDiff = mapped.growthDiff,
                growthImproved = mapped.growthImproved,
                currentAvgMonthly = mapped.currentAvgMonthly,
                previousAvgMonthly = mapped.previousAvgMonthly,
                avgMonthlyDiff = mapped.avgMonthlyDiff,
                avgMonthlyImproved = mapped.avgMonthlyImproved,
                avgMonthlyChangePercent = mapped.avgMonthlyChangePercent,
                speed = mapped.speed,
                stability = mapped.stability,
                stabilityCurrentVolatility = mapped.stabilityCurrentVolatility,
                stabilityPreviousVolatility = mapped.stabilityPreviousVolatility,
                bestMonthLabel = mapped.bestMonthLabel,
                bestMonthValue = mapped.bestMonthValue,
                currentChartData = mapped.currentChartData,
                previousChartData = mapped.previousChartData,
            )
        }
    }
}
