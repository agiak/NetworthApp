package com.agcoding.networkapp.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.usecase.GetAccountsUseCase
import com.agcoding.networkapp.account.domain.usecase.SeedDefaultAccountUseCase
import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.usecase.AddNetWorthEntryUseCase
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCase
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntriesUseCase
import com.agcoding.networkapp.home.presentation.mapper.NetWorthDomainToUiMapper
import com.agcoding.networkapp.home.presentation.mapper.NetWorthEntryToUiMapper
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetUserProfileUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMonthlyNetWorthUseCase: GetMonthlyNetWorthUseCase,
    private val getNetWorthEntriesUseCase: GetNetWorthEntriesUseCase,
    private val addNetWorthEntryUseCase: AddNetWorthEntryUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val seedDefaultAccountUseCase: SeedDefaultAccountUseCase,
    private val mapper: NetWorthDomainToUiMapper,
    private val entryMapper: NetWorthEntryToUiMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var dataJob: Job? = null
    private var recentEntriesJob: Job? = null

    private var cachedMonthlyData: List<MonthlyNetWorth> = emptyList()
    private var cachedRawEntries: List<NetWorthEntry> = emptyList()
    private var cachedAccounts: List<Account> = emptyList()
    private var currentCurrency: AppCurrency = AppCurrency.EUR

    init {
        viewModelScope.launch(ioDispatcher) { seedDefaultAccountUseCase() }
        loadData()
        loadRecentEntries()
        loadUserProfile()
        observeCurrency()
        observeAccounts()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ClearError -> _uiState.update { it.copy(error = null) }
            HomeIntent.LoadData -> {
                if (dataJob?.isActive != true) loadData()
                if (recentEntriesJob?.isActive != true) loadRecentEntries()
            }
            HomeIntent.ShowAddEntrySheet -> _uiState.update { it.copy(isAddEntrySheetVisible = true) }
            HomeIntent.HideAddEntrySheet -> _uiState.update { it.copy(isAddEntrySheetVisible = false, entryInput = "", noteInput = "", selectedDate = LocalDate.now(), entrySaved = false) }
            is HomeIntent.UpdateEntryInput -> _uiState.update { it.copy(entryInput = intent.value) }
            is HomeIntent.UpdateEntryDate -> _uiState.update { it.copy(selectedDate = intent.date) }
            is HomeIntent.UpdateEntryNote -> _uiState.update { it.copy(noteInput = intent.value) }
            is HomeIntent.SelectAccount   -> _uiState.update { it.copy(selectedAccountId = intent.accountId) }
            HomeIntent.SaveEntry -> saveEntry()
        }
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            getAccountsUseCase().collect { accounts ->
                cachedAccounts = accounts
                val currentId = _uiState.value.selectedAccountId
                val newId = if (accounts.any { it.id == currentId }) currentId else accounts.firstOrNull()?.id ?: 1L
                _uiState.update { it.copy(accounts = accounts, selectedAccountId = newId) }
                computeAccountBreakdown()
                applyRecentEntries(cachedRawEntries)
            }
        }
    }

    private fun computeAccountBreakdown() {
        if (cachedAccounts.isEmpty()) return
        val entriesByAccount = cachedRawEntries.groupBy { it.accountId }
        val balances = cachedAccounts.map { account ->
            val balance = entriesByAccount[account.id]
                ?.maxByOrNull { it.date }?.value ?: account.startingBalance
            account to balance
        }
        val total = balances.sumOf { it.second }.takeIf { it > 0 } ?: 1.0
        val sym = currentCurrency.symbol
        val breakdown = balances
            .map { (account, balance) ->
                AccountBreakdownUiItem(
                    id               = account.id,
                    name             = account.name,
                    colorHex         = account.colorHex,
                    formattedBalance = "$sym${String.format(Locale.US, "%,.0f", balance)}",
                    balanceRaw       = balance,
                    percentage       = (balance / total).toFloat().coerceIn(0f, 1f),
                )
            }
            .sortedByDescending { it.balanceRaw }
        _uiState.update { it.copy(accountBreakdown = breakdown) }
    }

    private fun observeCurrency() {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currency ->
                currentCurrency = currency
                _uiState.update { it.copy(currencySymbol = currency.symbol) }
                applyMonthlyData(cachedMonthlyData)
                applyRecentEntries(cachedRawEntries)
                computeAccountBreakdown()
                // Re-format target with new symbol
                val raw = _uiState.value.targetAmountRaw
                if (raw > 0.0) {
                    val formatted = "${currency.symbol}${String.format(java.util.Locale.US, "%,.0f", raw)}"
                    _uiState.update { it.copy(targetAmount = formatted) }
                }
            }
        }
    }

    private fun applyMonthlyData(monthlyData: List<MonthlyNetWorth>) {
        if (monthlyData.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading        = false,
                    currentNetWorth  = "${currentCurrency.symbol}0",
                    currentNetWorthRaw = 0.0,
                    changeThisMonth  = "",
                    changePercentage = "",
                    isPositiveChange = true,
                    lastUpdatedDate  = "",
                    chartData        = emptyList(),
                    insights         = emptyList(),
                    ytdGrowth        = "${currentCurrency.symbol}0",
                    ytdPercentage    = "0%",
                    avgPerMonth      = "${currentCurrency.symbol}0",
                    streakMonths     = 0,
                    isStreakPositive = true,
                    projectedNetWorth = "",
                    projectedNetWorthDate = "",
                    showProjection   = false,
                    error            = null,
                )
            }
            return
        }
        val displayData = mapper.map(monthlyData, currentCurrency)
        val rawNetWorth = monthlyData.sortedBy { it.yearMonth }.lastOrNull()?.value ?: 0.0
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                currentNetWorth = displayData.currentNetWorth,
                currentNetWorthRaw = rawNetWorth,
                changeThisMonth = displayData.changeThisMonth,
                changePercentage = displayData.changePercentage,
                isPositiveChange = displayData.isPositiveChange,
                lastUpdatedDate = displayData.lastUpdatedDate,
                chartData = displayData.chartData,
                insights = displayData.insights,
                ytdGrowth = displayData.ytdGrowth,
                ytdPercentage = displayData.ytdPercentage,
                avgPerMonth = displayData.avgPerMonth,
                streakMonths = displayData.streakMonths,
                isStreakPositive = displayData.isStreakPositive,
                projectedNetWorth = displayData.projectedNetWorth,
                projectedNetWorthDate = displayData.projectedNetWorthDate,
                showProjection = displayData.showProjection,
                error = null
            )
        }
    }

    private fun applyRecentEntries(entries: List<NetWorthEntry>) {
        val accountsById = cachedAccounts.associateBy { it.id }
        val recent = entries
            .sortedByDescending { it.date }
            .take(5)
            .map { entry ->
                val account = accountsById[entry.accountId]
                entryMapper.mapToUiModel(entry, currentCurrency).copy(
                    accountColorHex = account?.colorHex ?: "",
                    accountName     = account?.name ?: "",
                )
            }
        _uiState.update { it.copy(recentEntries = recent) }
    }

    private fun loadData() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            getMonthlyNetWorthUseCase().collect { result ->
                result.fold(
                    onSuccess = { monthlyData ->
                        cachedMonthlyData = monthlyData
                        applyMonthlyData(monthlyData)
                    },
                    onFailure = { error ->
                        Timber.e(error)
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }

    private fun loadRecentEntries() {
        recentEntriesJob?.cancel()
        recentEntriesJob = viewModelScope.launch {
            getNetWorthEntriesUseCase().collect { result ->
                result.fold(
                    onSuccess = { entries ->
                        cachedRawEntries = entries
                        applyRecentEntries(entries)
                        computeAccountBreakdown()
                    },
                    onFailure = { /* secondary data — silently ignored */ }
                )
            }
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            getUserProfileUseCase().collect { profile ->
                val initial = profile.name.take(1).uppercase()
                val formattedTarget = if (profile.targetAmount > 0.0) {
                    "${currentCurrency.symbol}${String.format(java.util.Locale.US, "%,.0f", profile.targetAmount)}"
                } else ""
                _uiState.update {
                    it.copy(
                        userName = profile.name,
                        userInitial = initial,
                        targetAmount = formattedTarget,
                        targetAmountRaw = profile.targetAmount,
                        hasGoal = profile.targetAmount > 0.0
                    )
                }
            }
        }
    }

    private fun saveEntry() {
        val input = _uiState.value.entryInput.toDoubleOrNull() ?: return
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSaving = true) }
            val entry = NetWorthEntry(value = input, date = _uiState.value.selectedDate, note = _uiState.value.noteInput, accountId = _uiState.value.selectedAccountId)
            addNetWorthEntryUseCase(entry).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, entrySaved = true) }
                    delay(900)
                    _uiState.update { it.copy(isAddEntrySheetVisible = false, entryInput = "", noteInput = "", selectedDate = LocalDate.now(), entrySaved = false) }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(isSaving = false, error = error.message) }
                }
            )
        }
    }
}
