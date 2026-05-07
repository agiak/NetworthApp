package com.agcoding.networkapp.analytics.presentation.prediction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.analytics.presentation.prediction.mapper.PredictionUiMapper
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
class PredictionViewModel @Inject constructor(
    private val getMonthlyNetWorthUseCase: GetMonthlyNetWorthUseCase,
    private val mapper: PredictionUiMapper
) : ViewModel() {

    private val _allData = MutableStateFlow<List<MonthlyNetWorth>>(emptyList())
    private val _uiState = MutableStateFlow(PredictionUiState())
    val uiState: StateFlow<PredictionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMonthlyNetWorthUseCase().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _allData.value = data
                        recompute(_uiState.value.selectedRange)
                    },
                    onFailure = { error ->
                        Timber.e(error)
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }

    fun onIntent(intent: PredictionIntent) {
        when (intent) {
            is PredictionIntent.SelectRange -> recompute(intent.range)
            PredictionIntent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun recompute(range: PredictionRange) {
        val result = mapper.map(_allData.value, range)
        _uiState.update {
            it.copy(
                isLoading = false,
                selectedRange = range,
                hasData = result.hasData,
                expectedValue = result.expectedValue,
                minimumValue = result.minimumValue,
                maximumValue = result.maximumValue,
                avgMonthlyGrowth = result.avgMonthlyGrowth,
                conservativeMonthlyRate = result.conservativeMonthlyRate,
                optimisticMonthlyRate = result.optimisticMonthlyRate,
                chartExpected = result.chartExpected,
                chartMinimum = result.chartMinimum,
                chartMaximum = result.chartMaximum,
                chartExpectedLabel = result.chartExpectedLabel,
                chartMinimumLabel = result.chartMinimumLabel,
                chartMaximumLabel = result.chartMaximumLabel,
                chartMidLabel = result.chartMidLabel,
                chartEndLabel = result.chartEndLabel
            )
        }
    }
}
