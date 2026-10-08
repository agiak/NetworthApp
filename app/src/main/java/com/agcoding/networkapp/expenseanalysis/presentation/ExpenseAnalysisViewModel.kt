package com.agcoding.networkapp.expenseanalysis.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.expenseanalysis.domain.model.CategorySource
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory
import com.agcoding.networkapp.expenseanalysis.domain.usecase.ExpenseAnalysisSource
import com.agcoding.networkapp.expenseanalysis.domain.usecase.ObserveExpenseAnalysisSourceUseCase
import com.agcoding.networkapp.expenseanalysis.domain.usecase.SetExpenseCategoryUseCase
import com.agcoding.networkapp.expenseanalysis.domain.usecase.analyzeExpenses
import com.agcoding.networkapp.expenseanalysis.presentation.mapper.ExpenseAnalysisUiMapper
import com.agcoding.networkapp.expenseanalysis.presentation.model.AccountFilterUiModel
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
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ExpenseAnalysisViewModel @Inject constructor(
    private val observeSourceUseCase: ObserveExpenseAnalysisSourceUseCase,
    private val setExpenseCategoryUseCase: SetExpenseCategoryUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val mapper: ExpenseAnalysisUiMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseAnalysisUiState())
    val uiState: StateFlow<ExpenseAnalysisUiState> = _uiState.asStateFlow()

    private val selectedAccountId = MutableStateFlow<Long?>(null)

    init {
        observeAnalysis()
    }

    fun onIntent(intent: ExpenseAnalysisIntent) {
        when (intent) {
            is ExpenseAnalysisIntent.SelectAccount  -> selectedAccountId.value = intent.accountId
            is ExpenseAnalysisIntent.ToggleCategory -> _uiState.update { state ->
                val expanded = state.expandedCategories
                state.copy(expandedCategories = if (intent.category in expanded) expanded - intent.category else expanded + intent.category)
            }
            is ExpenseAnalysisIntent.EditCategory   -> _uiState.update { state ->
                state.copy(editingExpense = state.categories.flatMap { it.expenses }.find { it.id == intent.expenseId })
            }
            is ExpenseAnalysisIntent.PickCategory   -> saveCategory(intent.category)
            ExpenseAnalysisIntent.ResetToAutomatic  -> saveCategory(null)
            ExpenseAnalysisIntent.DismissPicker     -> _uiState.update { it.copy(editingExpense = null) }
            ExpenseAnalysisIntent.NavigateToFixedExpenses -> { /* Handled in UI */ }
            ExpenseAnalysisIntent.ClearError        -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun observeAnalysis() {
        viewModelScope.launch {
            combine(observeSourceUseCase(), getAppCurrencyUseCase(), selectedAccountId) { result, currency, accountId ->
                Triple(result, currency, accountId)
            }.collect { (result, currency, accountId) ->
                result.fold(
                    onSuccess = { source -> publish(source, currency, accountId) },
                    onFailure = { error ->
                        Timber.e(error)
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    },
                )
            }
        }
    }

    private fun publish(source: ExpenseAnalysisSource, currency: AppCurrency, requestedAccountId: Long?) {
        // A deleted account falls back to everyone
        val accountId = requestedAccountId?.takeIf { id -> source.accounts.any { it.id == id } }
        val analysis = analyzeExpenses(source.expenses, source.accounts, accountId, source.categorizer)
        val showPerAccount = accountId == null && source.accounts.size > 1

        _uiState.update { state ->
            state.copy(
                isLoading           = false,
                hasAnyExpenses      = source.expenses.isNotEmpty(),
                accounts            = source.accounts.map { AccountFilterUiModel(it.id, it.name, it.colorHex) },
                selectedAccountId   = accountId,
                summary             = mapper.mapSummary(analysis, currency),
                categories          = analysis.categories.map { mapper.mapCategory(it, currency, source.accounts, showPerAccount) },
                uncertainCount      = analysis.uncertainCount,
                learnedChoicesCount = source.feedback.size,
            )
        }
    }

    /** Remembers the picked category (null = back to automatic) for the expense being edited. */
    private fun saveCategory(category: ExpenseCategory?) {
        val expense = _uiState.value.editingExpense ?: return
        _uiState.update { it.copy(editingExpense = null) }
        if (category == null && expense.source != CategorySource.MANUAL) return

        viewModelScope.launch(ioDispatcher) {
            setExpenseCategoryUseCase(expense.title, category).onFailure { error ->
                Timber.e(error)
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }
}
