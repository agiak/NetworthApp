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
import com.agcoding.networkapp.home.presentation.model.ChartPoint
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
class AnalyticsViewModel @Inject constructor(
    private val getMonthlyNetWorthUseCase: GetMonthlyNetWorthUseCase,
    private val getNetWorthEntriesUseCase: GetNetWorthEntriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val mapper: AnalyticsUiMapper,
) : ViewModel() {

    private val _allMonthlyData = MutableStateFlow<List<MonthlyNetWorth>>(emptyList())
    private val _rawEntries     = MutableStateFlow<List<NetWorthEntry>>(emptyList())
    private val _uiState        = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var currentCurrency: AppCurrency = AppCurrency.EUR
    private var targetAmountRaw: Double = 0.0

    init {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currency ->
                currentCurrency = currency
                recompute(_uiState.value.selectedFilter, _uiState.value.selectedAccountId)
            }
        }
        viewModelScope.launch {
            getMonthlyNetWorthUseCase().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _allMonthlyData.value = data
                        recompute(_uiState.value.selectedFilter, _uiState.value.selectedAccountId)
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
                result.fold(
                    onSuccess = { entries -> _rawEntries.value = entries },
                    onFailure = {}
                )
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
                targetAmountRaw = profile.targetAmount
                val hasGoal = targetAmountRaw > 0.0
                _uiState.update {
                    it.copy(
                        hasGoal = hasGoal,
                        targetAmountFormatted = if (hasGoal)
                            "${currentCurrency.symbol}${String.format(java.util.Locale.US, "%,.0f", targetAmountRaw)}"
                        else "",
                    )
                }
                recompute(_uiState.value.selectedFilter, _uiState.value.selectedAccountId)
            }
        }
    }

    fun onIntent(intent: AnalyticsIntent) {
        when (intent) {
            is AnalyticsIntent.SelectFilter  -> recompute(intent.filter, _uiState.value.selectedAccountId)
            is AnalyticsIntent.SelectAccount -> {
                _uiState.update { it.copy(selectedAccountId = intent.accountId) }
                recompute(_uiState.value.selectedFilter, intent.accountId)
            }
            AnalyticsIntent.ClearError       -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun recompute(filter: TimeFilter, accountId: Long?) {
        val monthlyData = if (accountId != null) {
            computeMonthlyForAccount(_rawEntries.value, accountId)
        } else {
            _allMonthlyData.value
        }
        val relevantEntries = if (accountId != null)
            _rawEntries.value.filter { it.accountId == accountId }
        else _rawEntries.value
        val result = mapper.map(monthlyData, filter, currentCurrency, relevantEntries)
        val comparison = buildComparison(_rawEntries.value, _uiState.value.accounts)
        _uiState.update {
            it.copy(
                accountComparison = comparison,
                isLoading               = false,
                selectedFilter          = filter,
                chartData               = result.chartData,
                chartStartLabel         = result.chartStartLabel,
                chartMidLabel           = result.chartMidLabel,
                chartEndLabel           = result.chartEndLabel,
                totalGrowth             = result.totalGrowth,
                totalGrowthPercent      = result.totalGrowthPercent,
                totalGrowthPositive     = result.totalGrowthPositive,
                avgMonthlyGrowth        = result.avgMonthlyGrowth,
                bestMonthLabel          = result.bestMonthLabel,
                bestMonthValue          = result.bestMonthValue,
                worstMonthLabel         = result.worstMonthLabel,
                worstMonthValue         = result.worstMonthValue,
                highestNetWorth         = result.highestNetWorth,
                highestNetWorthDate     = result.highestNetWorthDate,
                lowestNetWorth          = result.lowestNetWorth,
                lowestNetWorthDate      = result.lowestNetWorthDate,
                trendLabel              = result.trendLabel,
                trendDescription        = result.trendDescription,
                trendIsPositive         = result.trendIsPositive,
                trendIsNeutral          = result.trendIsNeutral,
                consistencyPercent      = result.consistencyPercent,
                consistencyDetail       = result.consistencyDetail,
                projectedNetWorth       = result.projectedNetWorth,
                projectedNetWorthDate   = result.projectedNetWorthDate,
                currentStreakLabel      = result.currentStreakLabel,
                currentStreakSubLabel   = result.currentStreakSubLabel,
                monthlyEntries              = result.monthlyEntries,
                hasData                     = result.hasData,
                currentNetWorthFormatted    = result.currentNetWorthFormatted,
                filterPeriodLabel           = result.filterPeriodLabel,
                goalProgressPercent         = if (targetAmountRaw > 0.0) {
                    val currentRaw = result.monthlyEntries.firstOrNull()?.rawValue ?: 0.0
                    ((currentRaw / targetAmountRaw) * 100).toInt().coerceIn(0, 100)
                } else 0,
            )
        }
    }

    // computeMonthlyForAccount is a top-level function in home.domain.usecase

    private fun buildComparison(
        entries: List<NetWorthEntry>,
        accounts: List<com.agcoding.networkapp.account.domain.model.Account>,
    ): List<AccountComparisonLine> {
        if (accounts.size < 2) return emptyList()
        return accounts.mapNotNull { account ->
            val monthly = computeMonthlyForAccount(entries, account.id)
            if (monthly.size < 2) return@mapNotNull null
            val values = monthly.map { it.value }
            val minVal = values.min()
            val maxVal = values.max()
            val range  = (maxVal - minVal).takeIf { it > 0 } ?: 1.0
            val points = monthly.mapIndexed { i, m ->
                ChartPoint(
                    x = i.toFloat() / (monthly.size - 1),
                    y = ((m.value - minVal) / range).toFloat(),
                )
            }
            AccountComparisonLine(account.name, account.colorHex, points)
        }
    }

}
