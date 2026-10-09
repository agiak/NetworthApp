package com.agcoding.networkapp.scenario.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory
import com.agcoding.networkapp.expenseanalysis.domain.usecase.ExpenseAnalysisSource
import com.agcoding.networkapp.expenseanalysis.domain.usecase.ObserveExpenseAnalysisSourceUseCase
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.monthlyCost
import com.agcoding.networkapp.scenario.domain.model.HomeExpenseType
import com.agcoding.networkapp.scenario.domain.model.ScenarioChanges
import com.agcoding.networkapp.scenario.domain.model.ScenarioResult
import com.agcoding.networkapp.scenario.domain.usecase.classifyHomeExpense
import com.agcoding.networkapp.scenario.domain.usecase.distribute
import com.agcoding.networkapp.scenario.domain.usecase.evaluateScenario
import com.agcoding.networkapp.scenario.domain.usecase.isPerCarExpense
import com.agcoding.networkapp.scenario.domain.usecase.scaleCarCost
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
import kotlin.math.ceil
import kotlin.math.roundToInt

@HiltViewModel
class ScenarioViewModel @Inject constructor(
    private val observeSourceUseCase: ObserveExpenseAnalysisSourceUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScenarioUiState())
    val uiState: StateFlow<ScenarioUiState> = _uiState.asStateFlow()

    private var currency: AppCurrency = AppCurrency.EUR
    private var source: ExpenseAnalysisSource? = null

    /** Today's home expenses of each kind. */
    private var homeGroups: Map<HomeExpenseType, List<FixedExpense>> = emptyMap()
    /** Today's expenses that come with owning a car. */
    private var carExpenses: List<FixedExpense> = emptyList()
    /** Car lines the user typed a value for: they no longer follow the number of cars. */
    private val editedCarLines = mutableSetOf<Long>()
    private var prefilled = false

    init {
        observeSource()
    }

    fun onIntent(intent: ScenarioIntent) {
        when (intent) {
            is ScenarioIntent.ToggleMove       -> _uiState.update { it.copy(moveEnabled = intent.enabled) }
            is ScenarioIntent.UpdateHomeCost   -> _uiState.update { state ->
                state.copy(homeCosts = state.homeCosts.map {
                    if (it.type == intent.type) it.copy(input = sanitizeAmountInput(intent.value)) else it
                })
            }
            is ScenarioIntent.UpdateMoveOneOff -> _uiState.update { it.copy(moveOneOffInput = sanitizeAmountInput(intent.value)) }

            is ScenarioIntent.ToggleCars       -> _uiState.update { it.copy(carsEnabled = intent.enabled) }
            is ScenarioIntent.SetCurrentCars   -> {
                val count = intent.count.coerceIn(0, ScenarioUiState.MAX_CARS)
                _uiState.update { it.copy(currentCars = count) }
                rescaleCarLines()
            }
            is ScenarioIntent.SetNewCars       -> {
                _uiState.update { it.copy(newCars = intent.count.coerceIn(0, ScenarioUiState.MAX_CARS)) }
                rescaleCarLines()
            }
            is ScenarioIntent.SetIncreasePercent -> {
                _uiState.update { it.copy(increasePercent = intent.percent) }
                rescaleCarLines()
            }
            is ScenarioIntent.UpdateCarLine    -> {
                editedCarLines += intent.expenseId
                _uiState.update { state ->
                    state.copy(carLines = state.carLines.map {
                        if (it.expenseId == intent.expenseId) it.copy(input = sanitizeAmountInput(intent.value)) else it
                    })
                }
            }
            is ScenarioIntent.UpdateNewCarMonthly -> _uiState.update { it.copy(newCarMonthlyInput = sanitizeAmountInput(intent.value)) }
            is ScenarioIntent.UpdateCarPayment -> _uiState.update { it.copy(carPaymentInput = sanitizeAmountInput(intent.value)) }
            is ScenarioIntent.UpdateCarOneOff  -> _uiState.update { it.copy(carOneOffInput = sanitizeAmountInput(intent.value)) }

            is ScenarioIntent.SelectPayer      -> _uiState.update { it.copy(payerAccountId = intent.accountId) }
            ScenarioIntent.Reset               -> {
                prefilled = false
                editedCarLines.clear()
                _uiState.update { ScenarioUiState(isLoading = false, currencySymbol = it.currencySymbol, payers = it.payers) }
                if (source != null) prefill()
            }
            ScenarioIntent.NavigateToSavingsPlanner -> { /* Handled in UI */ }
        }
        recompute()
    }

    private fun observeSource() {
        viewModelScope.launch {
            combine(observeSourceUseCase(), getAppCurrencyUseCase()) { result, appCurrency -> result to appCurrency }
                .collect { (result, appCurrency) ->
                    currency = appCurrency
                    result.fold(
                        onSuccess = { data ->
                            source = data
                            groupExpenses(data)
                            _uiState.update { state ->
                                state.copy(
                                    isLoading      = false,
                                    currencySymbol = appCurrency.symbol,
                                    payers         = data.accounts.map { PayerUiModel(it.id, it.name) },
                                    payerAccountId = state.payerAccountId?.takeIf { id -> data.accounts.any { it.id == id } },
                                )
                            }
                            if (!prefilled) prefill()
                            recompute()
                        },
                        onFailure = { error ->
                            Timber.e(error)
                            _uiState.update { it.copy(isLoading = false) }
                        },
                    )
                }
        }
    }

    private fun groupExpenses(data: ExpenseAnalysisSource) {
        val home = mutableMapOf<HomeExpenseType, MutableList<FixedExpense>>()
        val cars = mutableListOf<FixedExpense>()
        data.expenses.forEach { expense ->
            val homeType = classifyHomeExpense(expense.title)
            when {
                homeType != null -> home.getOrPut(homeType) { mutableListOf() } += expense
                data.categorizer.categorize(expense.title, expense.note).category == ExpenseCategory.TRANSPORT &&
                    isPerCarExpense(expense.title) -> cars += expense
            }
        }
        homeGroups = home
        carExpenses = cars.sortedByDescending { it.monthlyCost() }
    }

    /** Starts the scenario from today's costs. */
    private fun prefill() {
        prefilled = true
        val hasCars = carExpenses.isNotEmpty()
        _uiState.update { state ->
            state.copy(
                homeCosts = HomeExpenseType.entries.map { type ->
                    val current = homeGroups[type]?.sumOf { it.monthlyCost() }
                    HomeCostUiModel(
                        type             = type,
                        input            = current?.toInputString().orEmpty(),
                        formattedCurrent = current?.let { money(it) },
                    )
                },
                currentCars = if (hasCars) 1 else 0,
                newCars     = if (hasCars) 2 else 1,
                carLines    = carExpenses.map { expense ->
                    CarLineUiModel(expense.id, expense.title, money(expense.monthlyCost()), input = "")
                },
            )
        }
        rescaleCarLines()
    }

    /** Recomputes the car lines the user hasn't typed over. */
    private fun rescaleCarLines() {
        val state = _uiState.value
        val increase = state.increasePercent / 100.0
        _uiState.update {
            it.copy(carLines = it.carLines.map { line ->
                if (line.expenseId in editedCarLines) return@map line
                val current = carExpenses.find { e -> e.id == line.expenseId }?.monthlyCost() ?: 0.0
                line.copy(input = scaleCarCost(current, state.currentCars, state.newCars, increase).toInputString())
            })
        }
    }

    private fun recompute() {
        val data = source ?: return
        val state = _uiState.value
        val result = evaluateScenario(data.accounts, data.expenses, buildChanges(state))
        _uiState.update { it.copy(result = mapResult(result, data)) }
    }

    private fun buildChanges(state: ScenarioUiState): ScenarioChanges {
        val replacements = mutableMapOf<Long, Double>()
        val added = mutableListOf<Double>()
        var oneOff = 0.0

        if (state.moveEnabled) {
            state.homeCosts.forEach { cost ->
                // An empty field means the new home doesn't have that cost
                val value = cost.input.amount()
                val current = homeGroups[cost.type].orEmpty()
                if (current.isNotEmpty()) replacements += distribute(value, current)
                else if (value > 0.0) added += value
            }
            oneOff += state.moveOneOffInput.amount()
        }

        if (state.carsEnabled) {
            state.carLines.forEach { line -> replacements[line.expenseId] = line.input.amount() }
            if (state.needsNewCarEstimate) {
                val extraCars = (state.newCars - state.currentCars).coerceAtLeast(0)
                added += state.newCarMonthlyInput.amount() * extraCars
            }
            added += state.carPaymentInput.amount()
            oneOff += state.carOneOffInput.amount()
        }

        return ScenarioChanges(
            replacements    = replacements,
            addedMonthly    = added,
            addedAccountIds = state.payerAccountId?.let { listOf(it) }.orEmpty(),
            oneOffCost      = oneOff,
        )
    }

    private fun mapResult(result: ScenarioResult, data: ExpenseAnalysisSource): ScenarioResultUiModel {
        val before = result.before
        val after = result.after
        val savingsDifference = after.totalSavings - before.totalSavings
        val hasSalary = before.totalSalary > 0.0
        return ScenarioResultUiModel(
            hasSalary                   = hasSalary,
            formattedExpensesBefore     = money(before.totalFixedExpenses),
            formattedExpensesAfter      = money(after.totalFixedExpenses),
            formattedExpenseChange      = signed(result.monthlyExpenseChange),
            expensesIncrease            = result.monthlyExpenseChange > 0.005,
            formattedSavingsBefore      = money(before.totalSavings),
            formattedSavingsAfter       = money(after.totalSavings),
            isDeficitAfter              = after.totalSavings < 0.0,
            savingsRateBefore           = before.savingsRate?.let { (it * 100).roundToInt() },
            savingsRateAfter            = after.savingsRate?.let { (it * 100).roundToInt() },
            affordability               = result.affordability,
            formattedFiveYearDifference = if (hasSalary && abs(savingsDifference) > 0.005) signed(savingsDifference * 60, decimals = 0) else null,
            formattedOneOff             = result.oneOffCost.takeIf { it > 0.0 }?.let { money(it, decimals = 0) },
            monthsToCoverOneOff         = result.monthsToCoverOneOff?.let { ceil(it).toInt() },
            perAccount                  = if (data.accounts.size < 2 || !hasSalary) emptyList() else
                before.accounts.zip(after.accounts).map { (b, a) ->
                    AccountComparisonUiModel(
                        name                   = b.account.name,
                        colorHex               = b.account.colorHex,
                        formattedSavingsBefore = money(b.monthlySavings),
                        formattedSavingsAfter  = money(a.monthlySavings),
                        isDeficitAfter         = a.monthlySavings < 0.0,
                    )
                },
        )
    }

    private fun String.amount(): Double = toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    private fun money(value: Double, decimals: Int = 2): String {
        val sign = if (value < 0.0) "-" else ""
        return "$sign${currency.symbol}${String.format(Locale.US, "%,.${decimals}f", abs(value))}"
    }

    private fun signed(value: Double, decimals: Int = 2): String =
        (if (value > 0.0) "+" else "") + money(value, decimals)

    private fun Double.toInputString(): String {
        val rounded = Math.round(this * 100) / 100.0
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
               else String.format(Locale.US, "%.2f", rounded)
    }
}
