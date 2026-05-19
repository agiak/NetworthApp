package com.agcoding.networkapp.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.usecase.DeleteAccountUseCase
import com.agcoding.networkapp.account.domain.usecase.GetAccountsUseCase
import com.agcoding.networkapp.account.domain.usecase.UpdateAccountUseCase
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

data class AccountsUiData(
    val models: List<AccountUiModel>,
    val total: String,
    val best: AccountPerformance?,
    val worst: AccountPerformance?,
)

data class AccountPerformance(
    val id: Long,
    val name: String,
    val colorHex: String,
    val growthPctFormatted: String,
    val growthAbsFormatted: String,
    val isPositive: Boolean,
)

data class AccountUiModel(
    val id: Long,
    val name: String,
    val colorHex: String,
    val currentBalance: String,
    val currentBalanceRaw: Double,
    val change: String,
    val isPositiveChange: Boolean,
    val entryCount: Int,
    val startingBalance: Double,
)

data class AccountsOverviewUiState(
    val isLoading: Boolean = true,
    val accounts: List<AccountUiModel> = emptyList(),
    val totalNetWorth: String = "",
    val bestAccount: AccountPerformance? = null,
    val worstAccount: AccountPerformance? = null,
    val editingAccount: AccountUiModel? = null,
    val error: String? = null,
)

sealed interface AccountsOverviewIntent {
    data class DeleteAccount(val id: Long) : AccountsOverviewIntent
    data class StartEdit(val account: AccountUiModel) : AccountsOverviewIntent
    data class SaveEdit(val id: Long, val name: String, val colorHex: String) : AccountsOverviewIntent
    data object DismissEdit : AccountsOverviewIntent
    data object ClearError : AccountsOverviewIntent
}

@HiltViewModel
class AccountsOverviewViewModel @Inject constructor(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val netWorthRepository: NetWorthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsOverviewUiState())
    val uiState: StateFlow<AccountsOverviewUiState> = _uiState.asStateFlow()

    private var currentCurrency: AppCurrency = AppCurrency.EUR

    init {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currency -> currentCurrency = currency }
        }
        viewModelScope.launch {
            combine(getAccountsUseCase(), netWorthRepository.getEntries()) { accounts, entriesResult ->
                val entries = entriesResult.getOrElse { emptyList() }
                buildUiModels(accounts, entries)
            }.collect { data ->
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        accounts     = data.models,
                        totalNetWorth = data.total,
                        bestAccount  = data.best,
                        worstAccount = data.worst,
                    )
                }
            }
        }
    }

    fun onIntent(intent: AccountsOverviewIntent) {
        when (intent) {
            is AccountsOverviewIntent.DeleteAccount -> deleteAccount(intent.id)
            is AccountsOverviewIntent.StartEdit     -> _uiState.update { it.copy(editingAccount = intent.account) }
            is AccountsOverviewIntent.SaveEdit      -> saveEdit(intent.id, intent.name, intent.colorHex)
            AccountsOverviewIntent.DismissEdit      -> _uiState.update { it.copy(editingAccount = null) }
            AccountsOverviewIntent.ClearError       -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun deleteAccount(id: Long) {
        viewModelScope.launch(ioDispatcher) {
            try { deleteAccountUseCase(id) } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun saveEdit(id: Long, name: String, colorHex: String) {
        if (name.isBlank()) return
        val current = _uiState.value.accounts.find { it.id == id } ?: return
        viewModelScope.launch(ioDispatcher) {
            try {
                updateAccountUseCase(
                    com.agcoding.networkapp.account.domain.model.Account(
                        id = id,
                        name = name.trim(),
                        startingBalance = current.startingBalance,
                        colorHex = colorHex,
                    )
                )
                _uiState.update { it.copy(editingAccount = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun buildUiModels(
        accounts: List<Account>,
        allEntries: List<NetWorthEntry>,
    ): AccountsUiData {
        val sym = currentCurrency.symbol
        val entriesByAccount = allEntries.groupBy { it.accountId }

        val models = accounts.map { account ->
            val accountEntries = (entriesByAccount[account.id] ?: emptyList()).sortedBy { it.date }
            val current  = accountEntries.lastOrNull()?.value ?: account.startingBalance
            val previous = accountEntries.dropLast(1).lastOrNull()?.value ?: account.startingBalance
            val diff = current - previous
            AccountUiModel(
                id                = account.id,
                name              = account.name,
                colorHex          = account.colorHex,
                currentBalance    = "$sym${String.format(Locale.US, "%,.0f", current)}",
                currentBalanceRaw = current,
                change            = "${if (diff >= 0) "+" else ""}$sym${String.format(Locale.US, "%,.0f", diff)}",
                isPositiveChange  = diff >= 0,
                entryCount        = accountEntries.size,
                startingBalance   = account.startingBalance,
            )
        }

        val totalRaw = models.sumOf { it.currentBalanceRaw }
        val total    = "$sym${String.format(Locale.US, "%,.0f", totalRaw)}"

        // Best / worst account this year — pure computation, no side effects
        var best: AccountPerformance? = null
        var worst: AccountPerformance? = null
        if (accounts.size > 1) {
            val thisYear = LocalDate.now().year
            val performances = accounts.mapNotNull { account ->
                val sorted     = (entriesByAccount[account.id] ?: emptyList()).sortedBy { it.date }
                val yearEntries = sorted.filter { it.date.year == thisYear }
                val startVal   = yearEntries.firstOrNull()?.value ?: return@mapNotNull null
                val endVal     = sorted.lastOrNull()?.value ?: return@mapNotNull null
                val growthAbs  = endVal - startVal
                val growthPct  = if (startVal > 0) (growthAbs / startVal) * 100 else 0.0
                Triple(account, growthPct, growthAbs)
            }
            if (performances.size > 1) {
                fun toPerf(t: Triple<Account, Double, Double>?): AccountPerformance? = t?.let { (acc, pct, abs) ->
                    val sign = if (pct >= 0) "+" else ""
                    AccountPerformance(
                        id                 = acc.id,
                        name               = acc.name,
                        colorHex           = acc.colorHex,
                        growthPctFormatted = "$sign${String.format(Locale.US, "%.1f", pct)}%",
                        growthAbsFormatted = "$sign$sym${String.format(Locale.US, "%,.0f", abs)}",
                        isPositive         = pct >= 0,
                    )
                }
                best  = toPerf(performances.maxByOrNull { it.second })
                worst = toPerf(performances.minByOrNull { it.second })
            }
        }

        return AccountsUiData(models = models, total = total, best = best, worst = worst)
    }
}
