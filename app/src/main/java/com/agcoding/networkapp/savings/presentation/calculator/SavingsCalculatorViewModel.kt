package com.agcoding.networkapp.savings.presentation.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.savings.domain.usecase.projectSavings
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import com.agcoding.networkapp.shared.ui.utils.sanitizeAmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SavingsCalculatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsCalculatorUiState())
    val uiState: StateFlow<SavingsCalculatorUiState> = _uiState.asStateFlow()

    private var currency: AppCurrency = AppCurrency.EUR

    init {
        // Optional prefill (e.g. from the Savings Planner), passed in cents
        val prefillCents: Long = savedStateHandle["prefillAmountCents"] ?: 0L
        if (prefillCents > 0L) {
            _uiState.update { it.copy(monthlyAmountInput = (prefillCents / 100.0).toInputString()) }
        }
        observeCurrency()
    }

    fun onIntent(intent: SavingsCalculatorIntent) {
        when (intent) {
            is SavingsCalculatorIntent.UpdateMonthlyAmount ->
                _uiState.update { it.copy(monthlyAmountInput = sanitizeAmountInput(intent.value)) }
            is SavingsCalculatorIntent.UpdateAnnualReturn ->
                _uiState.update { it.copy(annualReturnInput = sanitizeAmountInput(intent.value).take(5)) }
            is SavingsCalculatorIntent.UpdateCustomYears ->
                _uiState.update { it.copy(customYearsInput = intent.value.filter(Char::isDigit).take(2)) }
        }
        recompute()
    }

    private fun observeCurrency() {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { appCurrency ->
                currency = appCurrency
                _uiState.update { it.copy(currencySymbol = appCurrency.symbol) }
                recompute()
            }
        }
    }

    private fun recompute() {
        val state = _uiState.value
        val monthly = state.monthlyAmountInput.toDoubleOrNull()?.takeIf { it > 0.0 }
        if (monthly == null) {
            _uiState.update { it.copy(projections = emptyList(), customProjection = null) }
            return
        }
        val annualReturn = state.annualReturnInput.toDoubleOrNull()?.coerceIn(0.0, MAX_RETURN_PERCENT) ?: 0.0
        val customYears = state.customYearsInput.toIntOrNull()?.takeIf { it in 1..MAX_YEARS }

        _uiState.update {
            it.copy(
                projections = PRESET_YEARS.map { years -> project(monthly, years, annualReturn) },
                customProjection = customYears?.let { years -> project(monthly, years, annualReturn, isCustom = true) },
            )
        }
    }

    private fun project(monthly: Double, years: Int, annualReturn: Double, isCustom: Boolean = false): SavingsProjectionUiModel {
        val projection = projectSavings(monthly, years, annualReturn)
        return SavingsProjectionUiModel(
            years             = years,
            formattedTotal    = money(projection.total),
            formattedDeposits = money(projection.deposits),
            formattedReturns  = if (annualReturn > 0.0) money(projection.returns) else null,
            isCustom          = isCustom,
        )
    }

    private fun money(value: Double) = "${currency.symbol}${String.format(Locale.US, "%,.0f", value)}"

    private fun Double.toInputString(): String =
        if (this == toLong().toDouble()) toLong().toString() else String.format(Locale.US, "%.2f", this)

    companion object {
        val PRESET_YEARS = listOf(1, 3, 5, 10, 15, 20)
        const val MAX_YEARS = 99
        const val MAX_RETURN_PERCENT = 100.0
    }
}
