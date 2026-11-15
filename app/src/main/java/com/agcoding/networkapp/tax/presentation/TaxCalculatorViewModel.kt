package com.agcoding.networkapp.tax.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import com.agcoding.networkapp.shared.ui.utils.sanitizeAmountInput
import com.agcoding.networkapp.tax.domain.computePartnershipTax
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class TaxCalculatorViewModel @Inject constructor(
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaxCalculatorUiState())
    val uiState: StateFlow<TaxCalculatorUiState> = _uiState.asStateFlow()

    private var currency: AppCurrency = AppCurrency.EUR

    init {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { appCurrency ->
                currency = appCurrency
                _uiState.update { it.copy(currencySymbol = appCurrency.symbol) }
                recompute()
            }
        }
    }

    fun onIntent(intent: TaxCalculatorIntent) {
        when (intent) {
            is TaxCalculatorIntent.UpdateTurnover ->
                _uiState.update { it.copy(turnoverInput = sanitizeAmountInput(intent.value).take(MAX_INPUT_LENGTH)) }
            is TaxCalculatorIntent.UpdateExpenses ->
                _uiState.update { it.copy(expensesInput = sanitizeAmountInput(intent.value).take(MAX_INPUT_LENGTH)) }
        }
        recompute()
    }

    private fun recompute() {
        val state = _uiState.value
        val turnover = state.turnoverInput.toDoubleOrNull()
        if (turnover == null) {
            _uiState.update { it.copy(result = null) }
            return
        }
        val tax = computePartnershipTax(turnover = turnover, expenses = state.expensesInput.toDoubleOrNull() ?: 0.0)
        _uiState.update {
            it.copy(
                result = TaxResultUiModel(
                    formattedTurnover       = money(tax.turnover),
                    formattedExpenses       = money(tax.expenses),
                    formattedProfit         = money(tax.profit),
                    formattedTax            = money(tax.tax),
                    formattedProfitAfterTax = money(tax.profitAfterTax),
                    formattedMonthlyReserve = money(tax.tax / 12.0),
                    isLoss                  = tax.isLoss,
                ),
            )
        }
    }

    private fun money(value: Double): String {
        val sign = if (value < 0.0) "-" else ""
        return "$sign${currency.symbol}${String.format(Locale.US, "%,.2f", abs(value))}"
    }

    private companion object {
        /** Up to hundreds of millions with cents */
        const val MAX_INPUT_LENGTH = 12
    }
}
