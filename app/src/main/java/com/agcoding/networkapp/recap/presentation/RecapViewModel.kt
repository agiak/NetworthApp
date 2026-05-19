package com.agcoding.networkapp.recap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.account.domain.usecase.GetAccountsUseCase
import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCase
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntriesUseCase
import com.agcoding.networkapp.recap.presentation.mapper.RecapUiMapper
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
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class RecapViewModel @Inject constructor(
    private val getMonthlyNetWorthUseCase: GetMonthlyNetWorthUseCase,
    private val getNetWorthEntriesUseCase: GetNetWorthEntriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val mapper: RecapUiMapper,
) : ViewModel() {

    private val _allData    = MutableStateFlow<List<MonthlyNetWorth>>(emptyList())
    private val _rawEntries = MutableStateFlow<List<NetWorthEntry>>(emptyList())
    private val _targetAmount = MutableStateFlow(0.0)
    private val _uiState = MutableStateFlow(RecapUiState())
    val uiState: StateFlow<RecapUiState> = _uiState.asStateFlow()

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
        viewModelScope.launch {
            getUserProfileUseCase().collect { profile ->
                _targetAmount.value = profile.targetAmount
                recompute()
            }
        }
    }

    fun onIntent(intent: RecapIntent) {
        when (intent) {
            is RecapIntent.SelectYear    -> { _uiState.update { it.copy(selectedYear = intent.year) }; recompute() }
            is RecapIntent.SelectAccount -> { _uiState.update { it.copy(selectedAccountId = intent.accountId) }; recompute() }
            RecapIntent.ClearError       -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun recompute() {
        val accountId = _uiState.value.selectedAccountId
        val data = if (accountId != null) computeMonthlyForAccount(_rawEntries.value, accountId) else _allData.value
        if (data.isEmpty() && accountId != null) {
            _uiState.update { it.copy(isLoading = false, hasData = false) }
            return
        }
        val result = mapper.map(data, _uiState.value.selectedYear, _targetAmount.value, currentCurrency)
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

    private fun computeMonthlyForAccount(entries: List<NetWorthEntry>, accountId: Long): List<MonthlyNetWorth> {
        val acctEntries = entries.filter { it.accountId == accountId }
        if (acctEntries.isEmpty()) return emptyList()
        val firstMonth = acctEntries.map { YearMonth.from(it.date) }.min()
        val lastMonth  = acctEntries.map { YearMonth.from(it.date) }.max()
        val byMonth    = acctEntries.groupBy { YearMonth.from(it.date) }
        var lastVal    = byMonth[firstMonth]!!.maxByOrNull { it.date }!!.value
        val result     = mutableListOf<MonthlyNetWorth>()
        var current    = firstMonth
        while (!current.isAfter(lastMonth)) {
            val month = byMonth[current]
            if (month != null) lastVal = month.maxByOrNull { it.date }!!.value
            result.add(MonthlyNetWorth(current, lastVal, month?.maxByOrNull { it.date }?.date ?: current.atDay(15), month == null))
            current = current.plusMonths(1)
        }
        return result
    }
}
