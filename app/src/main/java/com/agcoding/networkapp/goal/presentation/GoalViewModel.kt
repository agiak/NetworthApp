package com.agcoding.networkapp.goal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.goal.presentation.mapper.GoalUiMapper
import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCase
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val getMonthlyNetWorthUseCase: GetMonthlyNetWorthUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val mapper: GoalUiMapper
) : ViewModel() {

    private val _allData = MutableStateFlow<List<MonthlyNetWorth>>(emptyList())
    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

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
        viewModelScope.launch {
            getUserProfileUseCase().collect { profile ->
                val rawTarget = profile.targetAmount
                if (rawTarget > 0.0 && _uiState.value.targetInput.isEmpty()) {
                    _uiState.update { it.copy(targetInput = rawTarget.toLong().toString()) }
                    recompute()
                }
            }
        }
    }

    fun onIntent(intent: GoalIntent) {
        when (intent) {
            is GoalIntent.UpdateTargetInput -> {
                _uiState.update { it.copy(targetInput = intent.input) }
                recompute()
            }
            is GoalIntent.SelectTimeframe -> {
                _uiState.update { it.copy(selectedTimeframe = intent.timeframe) }
                recompute()
            }
            is GoalIntent.UpdateCustomYears -> {
                _uiState.update { it.copy(customYearsInput = intent.input) }
                recompute()
            }
            GoalIntent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun recompute() {
        val state = _uiState.value
        val targetAmount = state.targetInput.toDoubleOrNull() ?: run {
            _uiState.update { it.copy(isLoading = false, isInputValid = false) }
            return
        }
        if (targetAmount <= 0) {
            _uiState.update { it.copy(isLoading = false, isInputValid = false) }
            return
        }
        val timeframeMonths = effectiveTimeframeMonths()
        if (timeframeMonths <= 0) return

        val data = _allData.value
        if (data.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, hasHistoricalData = false, isInputValid = true) }
            return
        }

        val result = mapper.map(data, targetAmount, timeframeMonths, currentCurrency)
        _uiState.update {
            it.copy(
                isLoading = false,
                hasHistoricalData = true,
                isInputValid = true,
                currentNetWorthFormatted = result.currentNetWorthFormatted,
                currentNetWorthRaw = data.sortedBy { m -> m.yearMonth }.last().value,
                targetFormatted = result.targetFormatted,
                progressFraction = result.progressFraction,
                progressPercent = result.progressPercent,
                status = result.status,
                requiredMonthly = result.requiredMonthly,
                requiredYearly = result.requiredYearly,
                avgMonthlyGrowth = result.avgMonthlyGrowth,
                fasterByPercent = result.fasterByPercent,
                yearsAtCurrentPace = result.yearsAtCurrentPace,
                chartCurrentTrajectory = result.chartCurrentTrajectory,
                chartRequiredTrajectory = result.chartRequiredTrajectory,
                chartEndLabel = result.chartEndLabel
            )
        }
    }

    private fun effectiveTimeframeMonths(): Int {
        val state = _uiState.value
        return if (state.selectedTimeframe == GoalTimeframe.CUSTOM) {
            (state.customYearsInput.toIntOrNull() ?: 5).coerceIn(1, 30) * 12
        } else {
            state.selectedTimeframe.fixedMonths ?: 36
        }
    }
}
