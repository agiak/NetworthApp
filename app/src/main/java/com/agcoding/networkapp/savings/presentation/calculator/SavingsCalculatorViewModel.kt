package com.agcoding.networkapp.savings.presentation.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.savings.domain.model.SavingsPlan
import com.agcoding.networkapp.savings.domain.model.selection
import com.agcoding.networkapp.savings.domain.usecase.GetSavingsPlanUseCase
import com.agcoding.networkapp.savings.domain.usecase.projectSavings
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import com.agcoding.networkapp.shared.ui.utils.sanitizeAmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class SavingsCalculatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSavingsPlanUseCase: GetSavingsPlanUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsCalculatorUiState())
    val uiState: StateFlow<SavingsCalculatorUiState> = _uiState.asStateFlow()

    private var currency: AppCurrency = AppCurrency.EUR
    private var plan: SavingsPlan? = null

    init {
        // Optional prefill, passed in cents: start from that amount instead of the salaries
        val prefillCents: Long = savedStateHandle["prefillAmountCents"] ?: 0L
        if (prefillCents > 0L) {
            _uiState.update {
                it.copy(source = SavingsSource.MANUAL, monthlyAmountInput = (prefillCents / 100.0).toInputString())
            }
        }
        observePlan()
    }

    fun onIntent(intent: SavingsCalculatorIntent) {
        when (intent) {
            is SavingsCalculatorIntent.SelectSource ->
                _uiState.update { it.copy(source = intent.source) }
            is SavingsCalculatorIntent.SelectAccount ->
                _uiState.update { it.copy(selectedAccountId = intent.accountId) }
            is SavingsCalculatorIntent.UpdateMonthlyAmount ->
                _uiState.update { it.copy(monthlyAmountInput = sanitizeAmountInput(intent.value)) }
            is SavingsCalculatorIntent.UpdateAnnualReturn ->
                _uiState.update { it.copy(annualReturnInput = sanitizeAmountInput(intent.value).take(5)) }
            is SavingsCalculatorIntent.UpdateCustomYears ->
                _uiState.update { it.copy(customYearsInput = intent.value.filter(Char::isDigit).take(2)) }
            SavingsCalculatorIntent.NavigateToSavingsPlanner -> { /* Handled in UI */ }
        }
        recompute()
    }

    private fun observePlan() {
        viewModelScope.launch {
            combine(getSavingsPlanUseCase(), getAppCurrencyUseCase()) { result, appCurrency -> result to appCurrency }
                .collect { (result, appCurrency) ->
                    currency = appCurrency
                    plan = result.onFailure { Timber.e(it) }.getOrNull()
                    _uiState.update { state ->
                        val accounts = plan?.accounts.orEmpty().map { CalculatorAccountUiModel(it.account.id, it.account.name) }
                        state.copy(
                            currencySymbol    = appCurrency.symbol,
                            accounts          = accounts,
                            // A deleted account falls back to everyone
                            selectedAccountId = state.selectedAccountId?.takeIf { id -> accounts.any { it.id == id } },
                        )
                    }
                    recompute()
                }
        }
    }

    private fun recompute() {
        val state = _uiState.value
        val selection = plan?.selection(state.selectedAccountId)
        val breakdown = selection?.let {
            PlanBreakdownUiModel(
                formattedSalary        = money(it.monthlySalary, decimals = 2),
                formattedFixedExpenses = money(it.monthlyFixedExpenses, decimals = 2),
                formattedSavings       = money(it.monthlySavings, decimals = 2),
                hasSalary              = it.hasSalary,
                isDeficit              = it.monthlySavings < 0.0,
                accountsWithoutSalary  = it.accountsWithoutSalary.map { account -> account.name },
            )
        }

        val monthly = when (state.source) {
            SavingsSource.PLAN   -> selection?.takeIf { it.hasSalary }?.monthlySavings
            SavingsSource.MANUAL -> state.monthlyAmountInput.toDoubleOrNull()
        }?.takeIf { it > 0.0 }

        if (monthly == null) {
            _uiState.update { it.copy(planBreakdown = breakdown, projections = emptyList(), customProjection = null) }
            return
        }
        val annualReturn = state.annualReturnInput.toDoubleOrNull()?.coerceIn(0.0, MAX_RETURN_PERCENT) ?: 0.0
        val customYears = state.customYearsInput.toIntOrNull()?.takeIf { it in 1..MAX_YEARS }

        _uiState.update {
            it.copy(
                planBreakdown    = breakdown,
                projections      = PRESET_YEARS.map { years -> project(monthly, years, annualReturn) },
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

    private fun money(value: Double, decimals: Int = 0): String {
        val sign = if (value < 0.0) "-" else ""
        return "$sign${currency.symbol}${String.format(Locale.US, "%,.${decimals}f", abs(value))}"
    }

    private fun Double.toInputString(): String =
        if (this == toLong().toDouble()) toLong().toString() else String.format(Locale.US, "%.2f", this)

    companion object {
        val PRESET_YEARS = listOf(1, 3, 5, 10, 15, 20)
        const val MAX_YEARS = 99
        const val MAX_RETURN_PERCENT = 100.0
    }
}
