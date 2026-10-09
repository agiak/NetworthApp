package com.agcoding.networkapp.scenario.presentation

import com.agcoding.networkapp.scenario.domain.model.Affordability
import com.agcoding.networkapp.scenario.domain.model.HomeExpenseType

data class HomeCostUiModel(
    val type: HomeExpenseType,
    val input: String,
    /** Today's monthly cost of this kind, or null if there is none. */
    val formattedCurrent: String?,
)

data class CarLineUiModel(
    val expenseId: Long,
    val title: String,
    val formattedCurrent: String,
    val input: String,
)

data class PayerUiModel(
    val id: Long,
    val name: String,
)

data class AccountComparisonUiModel(
    val name: String,
    val colorHex: String,
    val formattedSavingsBefore: String,
    val formattedSavingsAfter: String,
    val isDeficitAfter: Boolean,
)

data class ScenarioResultUiModel(
    val hasSalary: Boolean,
    val formattedExpensesBefore: String,
    val formattedExpensesAfter: String,
    /** Signed, e.g. "+€772.50" */
    val formattedExpenseChange: String,
    val expensesIncrease: Boolean,
    val formattedSavingsBefore: String,
    val formattedSavingsAfter: String,
    val isDeficitAfter: Boolean,
    val savingsRateBefore: Int?,
    val savingsRateAfter: Int?,
    val affordability: Affordability,
    /** Signed difference in savings over five years. */
    val formattedFiveYearDifference: String?,
    val formattedOneOff: String?,
    /** Months of the new savings that the one-off costs take, rounded up. */
    val monthsToCoverOneOff: Int?,
    val perAccount: List<AccountComparisonUiModel>,
)

data class ScenarioUiState(
    val isLoading: Boolean = true,
    val currencySymbol: String = "€",

    val moveEnabled: Boolean = false,
    val homeCosts: List<HomeCostUiModel> = emptyList(),
    val moveOneOffInput: String = "",

    val carsEnabled: Boolean = false,
    val currentCars: Int = 1,
    val newCars: Int = 2,
    val increasePercent: Int = 50,
    val carLines: List<CarLineUiModel> = emptyList(),
    val newCarMonthlyInput: String = "",
    val carPaymentInput: String = "",
    val carOneOffInput: String = "",

    val payers: List<PayerUiModel> = emptyList(),
    /** null = new expenses are shared by everyone */
    val payerAccountId: Long? = null,

    val result: ScenarioResultUiModel? = null,
) {
    /** Without today's car costs to scale, the user estimates the cost of each new car. */
    val needsNewCarEstimate: Boolean get() = carLines.isEmpty() || currentCars == 0

    companion object {
        val INCREASE_OPTIONS = listOf(50, 60, 75, 100)
        const val MAX_CARS = 5
    }
}
