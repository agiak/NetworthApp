package com.agcoding.networkapp.savings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.account.domain.usecase.UpdateAccountSalaryUseCase
import com.agcoding.networkapp.savings.domain.usecase.GetSavingsPlanUseCase
import com.agcoding.networkapp.savings.presentation.mapper.SavingsPlanUiMapper
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import com.agcoding.networkapp.shared.ui.utils.sanitizeAmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SavingsPlannerViewModel @Inject constructor(
    private val getSavingsPlanUseCase: GetSavingsPlanUseCase,
    private val updateAccountSalaryUseCase: UpdateAccountSalaryUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val mapper: SavingsPlanUiMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsPlannerUiState())
    val uiState: StateFlow<SavingsPlannerUiState> = _uiState.asStateFlow()

    init {
        observePlan()
    }

    fun onIntent(intent: SavingsPlannerIntent) {
        when (intent) {
            is SavingsPlannerIntent.EditSalary -> {
                val account = _uiState.value.accounts.find { it.accountId == intent.accountId } ?: return
                _uiState.update {
                    it.copy(
                        editingAccount = account,
                        salaryInput = if (account.hasSalary) account.salaryRaw.toInputString() else "",
                    )
                }
            }
            is SavingsPlannerIntent.UpdateSalaryInput -> _uiState.update { it.copy(salaryInput = sanitizeAmountInput(intent.value)) }
            SavingsPlannerIntent.SaveSalary           -> saveSalary()
            SavingsPlannerIntent.DismissSalaryDialog  -> _uiState.update { it.copy(editingAccount = null, salaryInput = "") }
            SavingsPlannerIntent.NavigateToFixedExpenses -> { /* Handled in UI */ }
            SavingsPlannerIntent.NavigateToCalculator    -> { /* Handled in UI */ }
            SavingsPlannerIntent.ClearError           -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun observePlan() {
        viewModelScope.launch {
            combine(getSavingsPlanUseCase(), getAppCurrencyUseCase()) { result, currency -> result to currency }
                .collect { (result, currency) ->
                    result.fold(
                        onSuccess = { plan ->
                            _uiState.update { state ->
                                state.copy(
                                    isLoading         = false,
                                    currencySymbol    = currency.symbol,
                                    accounts          = plan.accounts.map { mapper.mapAccount(it, currency) },
                                    summary           = mapper.mapSummary(plan, currency),
                                    totalExpenseCount = plan.totalExpenseCount,
                                )
                            }
                        },
                        onFailure = { error ->
                            Timber.e(error)
                            _uiState.update { it.copy(isLoading = false, error = error.message) }
                        }
                    )
                }
        }
    }

    private fun saveSalary() {
        val state = _uiState.value
        val account = state.editingAccount ?: return
        // Empty input clears the salary
        val salary = if (state.salaryInput.isBlank()) 0.0 else state.salaryInput.toDoubleOrNull() ?: return

        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSaving = true) }
            runCatching { updateAccountSalaryUseCase(account.accountId, salary) }.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, editingAccount = null, salaryInput = "") }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(isSaving = false, error = error.message) }
                }
            )
        }
    }

    private fun Double.toInputString(): String =
        if (this == toLong().toDouble()) toLong().toString() else String.format(Locale.US, "%.2f", this)
}
