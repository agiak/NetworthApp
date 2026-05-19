package com.agcoding.networkapp.account.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.agcoding.networkapp.account.domain.usecase.GetAccountsUseCase
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntriesUseCase
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import com.agcoding.networkapp.shared.navigation.AccountDetailRoute
import com.agcoding.networkapp.shared.ui.model.EntryUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class AccountDetailUiState(
    val isLoading: Boolean = true,
    val accountName: String = "",
    val accountColorHex: String = "",
    val currentBalance: String = "",
    val change: String = "",
    val isPositiveChange: Boolean = true,
    val totalGrowth: String = "",
    val totalGrowthPercent: String = "",
    val totalGrowthPositive: Boolean = true,
    val avgPerMonth: String = "",
    val entryCount: Int = 0,
    val chartData: List<ChartPoint> = emptyList(),
    val recentEntries: List<EntryUiModel> = emptyList(),
    val hasData: Boolean = false,
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNetWorthEntriesUseCase: GetNetWorthEntriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
) : ViewModel() {

    private val accountId: Long = savedStateHandle.toRoute<AccountDetailRoute>().accountId

    private val _uiState = MutableStateFlow(AccountDetailUiState())
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    private var currentCurrency: AppCurrency = AppCurrency.EUR

    init {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currentCurrency = it }
        }
        viewModelScope.launch {
            combine(
                getAccountsUseCase(),
                getNetWorthEntriesUseCase(),
            ) { accounts, entriesResult ->
                val account = accounts.find { it.id == accountId } ?: return@combine
                val allEntries = entriesResult.getOrElse { emptyList() }
                val entries = allEntries.filter { it.accountId == accountId }.sortedBy { it.date }
                buildState(account.name, account.colorHex, entries)
            }.collect {}
        }
        // Reactive on entries + accounts separately for live updates
        viewModelScope.launch {
            combine(getAccountsUseCase(), getNetWorthEntriesUseCase()) { accts, entriesResult ->
                accts to entriesResult
            }.collect { (accounts, entriesResult) ->
                val account = accounts.find { it.id == accountId } ?: return@collect
                val entries = entriesResult.getOrElse { emptyList() }
                    .filter { it.accountId == accountId }
                    .sortedBy { it.date }
                buildState(account.name, account.colorHex, entries)
            }
        }
    }

    private fun buildState(name: String, colorHex: String, entries: List<NetWorthEntry>) {
        val sym = currentCurrency.symbol
        if (entries.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, accountName = name, accountColorHex = colorHex, hasData = false) }
            return
        }

        val current  = entries.last().value
        val previous = entries.dropLast(1).lastOrNull()?.value ?: current
        val change   = current - previous
        val first    = entries.first().value
        val totalGrowthAbs = current - first
        val totalGrowthPct = if (first > 0) (totalGrowthAbs / first) * 100 else 0.0

        val monthlyGroups = entries.groupBy { YearMonth.from(it.date) }
        val monthlyValues = monthlyGroups.entries
            .sortedBy { it.key }
            .map { (_, es) -> es.maxByOrNull { it.date }!!.value }
        val avgPerMonth = if (monthlyValues.size > 1) {
            val diffs = monthlyValues.zipWithNext { a, b -> b - a }
            diffs.average()
        } else 0.0

        // Chart points
        val minVal = entries.minOf { it.value }
        val maxVal = entries.maxOf { it.value }
        val range = (maxVal - minVal).takeIf { it > 0 } ?: 1.0
        val chartData = entries.mapIndexed { i, e ->
            ChartPoint(
                x = i.toFloat() / (entries.size - 1).coerceAtLeast(1),
                y = ((e.value - minVal) / range).toFloat(),
            )
        }

        // Recent entries (last 5)
        val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        val recent = entries.reversed().take(5).map { e ->
            EntryUiModel(
                id             = e.id,
                formattedDate  = e.date.format(fmt),
                formattedValue = "$sym${String.format(Locale.US, "%,.0f", e.value)}",
                accountColorHex = colorHex,
            )
        }

        _uiState.update {
            it.copy(
                isLoading          = false,
                accountName        = name,
                accountColorHex    = colorHex,
                currentBalance     = "$sym${String.format(Locale.US, "%,.0f", current)}",
                change             = "${if (change >= 0) "+" else ""}$sym${String.format(Locale.US, "%,.0f", change)}",
                isPositiveChange   = change >= 0,
                totalGrowth        = "${if (totalGrowthAbs >= 0) "+" else ""}$sym${String.format(Locale.US, "%,.0f", totalGrowthAbs)}",
                totalGrowthPercent = "${if (totalGrowthPct >= 0) "+" else ""}${String.format(Locale.US, "%.1f", totalGrowthPct)}%",
                totalGrowthPositive = totalGrowthAbs >= 0,
                avgPerMonth        = "${if (avgPerMonth >= 0) "+" else ""}$sym${String.format(Locale.US, "%,.0f", avgPerMonth)}",
                entryCount         = entries.size,
                chartData          = chartData,
                recentEntries      = recent,
                hasData            = true,
            )
        }
    }
}
