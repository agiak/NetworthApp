package com.agcoding.networkapp.fixedexpenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.usecase.GetAccountsUseCase
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpenseSortOption
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.domain.usecase.AddFixedExpenseUseCase
import com.agcoding.networkapp.fixedexpenses.domain.usecase.DeleteFixedExpenseUseCase
import com.agcoding.networkapp.fixedexpenses.domain.usecase.GetFixedExpensesUseCase
import com.agcoding.networkapp.fixedexpenses.domain.usecase.UpdateFixedExpenseUseCase
import com.agcoding.networkapp.fixedexpenses.presentation.mapper.FixedExpenseDomainToUiMapper
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class FixedExpensesViewModel @Inject constructor(
    private val getFixedExpensesUseCase: GetFixedExpensesUseCase,
    private val addFixedExpenseUseCase: AddFixedExpenseUseCase,
    private val updateFixedExpenseUseCase: UpdateFixedExpenseUseCase,
    private val deleteFixedExpenseUseCase: DeleteFixedExpenseUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val mapper: FixedExpenseDomainToUiMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FixedExpensesUiState())
    val uiState: StateFlow<FixedExpensesUiState> = _uiState.asStateFlow()

    private var currentCurrency: AppCurrency = AppCurrency.EUR
    private var cachedExpenses: List<FixedExpense> = emptyList()
    private var cachedAccounts: List<Account> = emptyList()

    init {
        loadExpenses()
        loadAccounts()
        observeCurrency()
    }

    fun onIntent(intent: FixedExpensesIntent) {
        when (intent) {
            FixedExpensesIntent.ShowAddSheet -> _uiState.update {
                it.copy(
                    isSheetVisible = true,
                    editingExpense = null,
                    titleInput = "",
                    noteInput = "",
                    costInput = "",
                    dateInput = null,
                    recurrenceInput = RecurrenceType.MONTHLY,
                    selectedAccountIds = emptyList(),
                )
            }
            is FixedExpensesIntent.ShowEditSheet -> _uiState.update {
                it.copy(
                    isSheetVisible = true,
                    editingExpense = intent.expense,
                    titleInput = intent.expense.title,
                    noteInput = intent.expense.note,
                    costInput = intent.expense.costRaw.let { v ->
                        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
                    },
                    dateInput = null,
                    recurrenceInput = intent.expense.recurrence,
                    selectedAccountIds = intent.expense.accountIds,
                )
            }
            FixedExpensesIntent.HideSheet -> _uiState.update {
                it.copy(isSheetVisible = false, editingExpense = null)
            }
            is FixedExpensesIntent.UpdateTitle     -> _uiState.update { it.copy(titleInput = intent.value) }
            is FixedExpensesIntent.UpdateNote      -> _uiState.update { it.copy(noteInput = intent.value) }
            is FixedExpensesIntent.UpdateCost      -> _uiState.update { it.copy(costInput = intent.value) }
            is FixedExpensesIntent.UpdateDate      -> _uiState.update { it.copy(dateInput = intent.date) }
            is FixedExpensesIntent.UpdateRecurrence -> _uiState.update { it.copy(recurrenceInput = intent.recurrence) }
            is FixedExpensesIntent.ToggleAccount   -> toggleAccount(intent.accountId)
            FixedExpensesIntent.SelectAllAccounts  -> _uiState.update { it.copy(selectedAccountIds = emptyList()) }
            is FixedExpensesIntent.SetSortOption   -> {
                _uiState.update { it.copy(sortOption = intent.option) }
                rebuildUi(cachedExpenses, cachedAccounts, sortOption = intent.option)
            }
            is FixedExpensesIntent.ToggleFilterAccount -> toggleFilterAccount(intent.accountId)
            FixedExpensesIntent.ClearFilterAccounts    -> {
                _uiState.update { it.copy(filterAccountIds = emptySet()) }
                rebuildUi(cachedExpenses, cachedAccounts)
            }
            FixedExpensesIntent.Save         -> save()
            is FixedExpensesIntent.Delete    -> delete(intent.id)
            FixedExpensesIntent.ClearError   -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun toggleAccount(accountId: Long) {
        _uiState.update { state ->
            val updated = if (accountId in state.selectedAccountIds)
                state.selectedAccountIds - accountId
            else
                state.selectedAccountIds + accountId
            state.copy(selectedAccountIds = updated)
        }
    }

    private fun toggleFilterAccount(accountId: Long) {
        val newFilter = _uiState.value.filterAccountIds.let { current ->
            if (accountId in current) current - accountId else current + accountId
        }
        _uiState.update { it.copy(filterAccountIds = newFilter) }
        rebuildUi(cachedExpenses, cachedAccounts, filterAccountIds = newFilter)
    }

    private fun loadExpenses() {
        viewModelScope.launch {
            getFixedExpensesUseCase().collect { result ->
                result.fold(
                    onSuccess = { expenses ->
                        cachedExpenses = expenses
                        rebuildUi(expenses, cachedAccounts)
                    },
                    onFailure = { error ->
                        Timber.e(error)
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            getAccountsUseCase().collect { accounts ->
                cachedAccounts = accounts
                _uiState.update { it.copy(availableAccounts = accounts) }
                rebuildUi(cachedExpenses, accounts)
            }
        }
    }

    private fun observeCurrency() {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currency ->
                currentCurrency = currency
                _uiState.update { it.copy(currencySymbol = currency.symbol) }
                rebuildUi(cachedExpenses, cachedAccounts)
            }
        }
    }

    private fun rebuildUi(
        expenses: List<FixedExpense>,
        accounts: List<Account>,
        sortOption: FixedExpenseSortOption = _uiState.value.sortOption,
        filterAccountIds: Set<Long> = _uiState.value.filterAccountIds,
    ) {
        val filtered     = applyAccountFilter(expenses, filterAccountIds)
        val uiModels     = filtered.sorted(sortOption).map { mapper.map(it, currentCurrency, accounts) }
        // Summary always uses ALL expenses so totals never change when a filter is applied
        val accountStats = mapper.computeAccountStats(expenses, accounts, currentCurrency)
        _uiState.update { state ->
            state.copy(
                isLoading          = false,
                expenses           = uiModels,
                totalExpensesCount = expenses.size,
                totalFormatted     = mapper.formatMonthlyTotal(expenses, currentCurrency),
                yearlyFormatted    = mapper.formatYearlyTotal(expenses, currentCurrency),
                accountStats       = accountStats,
            )
        }
    }

    private fun applyAccountFilter(
        expenses: List<FixedExpense>,
        filterAccountIds: Set<Long>,
    ): List<FixedExpense> {
        if (filterAccountIds.isEmpty()) return expenses
        return expenses.filter { expense ->
            // Expenses with no specific account (= all) always appear.
            // Expenses with specific accounts appear only if they share one with the filter.
            expense.accountIds.isEmpty() || expense.accountIds.any { it in filterAccountIds }
        }
    }

    private fun save() {
        val state = _uiState.value
        val cost = state.costInput.toDoubleOrNull() ?: return
        if (state.titleInput.isBlank()) return

        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSaving = true) }
            val editing = state.editingExpense

            val existingDate: LocalDate? = if (editing != null) {
                cachedExpenses.firstOrNull { it.id == editing.id }?.date
            } else null

            val expense = FixedExpense(
                id = editing?.id ?: 0,
                title = state.titleInput.trim(),
                note = state.noteInput.trim(),
                cost = cost,
                date = state.dateInput ?: existingDate,
                recurrence = state.recurrenceInput,
                accountIds = state.selectedAccountIds,
            )
            val result = if (editing == null) addFixedExpenseUseCase(expense)
                         else updateFixedExpenseUseCase(expense)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, isSheetVisible = false, editingExpense = null) }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(isSaving = false, error = error.message) }
                }
            )
        }
    }

    private fun delete(id: Long) {
        viewModelScope.launch(ioDispatcher) {
            deleteFixedExpenseUseCase(id).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSheetVisible = false, editingExpense = null) }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    private fun List<FixedExpense>.sorted(option: FixedExpenseSortOption): List<FixedExpense> =
        when (option) {
            FixedExpenseSortOption.COST_HIGH -> sortedByDescending { it.cost }
            FixedExpenseSortOption.COST_LOW  -> sortedBy { it.cost }
        }
}
