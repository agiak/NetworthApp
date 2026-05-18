package com.agcoding.networkapp.recap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCase
import com.agcoding.networkapp.recap.presentation.mapper.RecapUiMapper
import com.agcoding.networkapp.settings.domain.usecase.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RecapViewModel @Inject constructor(
    private val getMonthlyNetWorthUseCase: GetMonthlyNetWorthUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val mapper: RecapUiMapper
) : ViewModel() {

    private val _allData = MutableStateFlow<List<MonthlyNetWorth>>(emptyList())
    private val _targetAmount = MutableStateFlow(0.0)
    private val _uiState = MutableStateFlow(RecapUiState())
    val uiState: StateFlow<RecapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMonthlyNetWorthUseCase().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _allData.value = data
                        val years = data.map { it.yearMonth.year }.distinct().sortedDescending()
                        val currentYear = LocalDate.now().year
                        val defaultYear = if (years.contains(currentYear)) currentYear
                                          else years.firstOrNull() ?: currentYear
                        _uiState.update { it.copy(availableYears = years, selectedYear = defaultYear) }
                        recompute()
                    },
                    onFailure = { error ->
                        Timber.e(error)
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
        viewModelScope.launch {
            getUserProfileUseCase().collect { profile ->
                _targetAmount.value = profile.targetAmount
                recompute()
            }
        }
    }

    fun onIntent(intent: RecapIntent) {
        when (intent) {
            is RecapIntent.SelectYear -> {
                _uiState.update { it.copy(selectedYear = intent.year) }
                recompute()
            }
            RecapIntent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun recompute() {
        val result = mapper.map(_allData.value, _uiState.value.selectedYear, _targetAmount.value)
        _uiState.update {
            it.copy(
                isLoading = false,
                hasData = result.hasData,
                totalGrowthFormatted = result.totalGrowthFormatted,
                totalGrowthPercent = result.totalGrowthPercent,
                totalGrowthPositive = result.totalGrowthPositive,
                startValue = result.startValue,
                endValue = result.endValue,
                monthsTracked = result.monthsTracked,
                bestMonthLabel = result.bestMonthLabel,
                bestMonthValue = result.bestMonthValue,
                worstMonthLabel = result.worstMonthLabel,
                worstMonthValue = result.worstMonthValue,
                avgMonthlyGrowth = result.avgMonthlyGrowth,
                biggestJump = result.biggestJump,
                hasGoal = result.hasGoal,
                goalProgress = result.goalProgress,
                goalProgressPercent = result.goalProgressPercent,
                goalYearContribution = result.goalYearContribution,
                trend = result.trend,
                isNewAllTimeHigh = result.isNewAllTimeHigh,
                chartData = result.chartData,
                chartStartLabel = result.chartStartLabel,
                chartEndLabel = result.chartEndLabel,
                monthlyBreakdown = result.monthlyBreakdown
            )
        }
    }
}
