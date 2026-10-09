package com.agcoding.networkapp.scenario.domain.model

import com.agcoding.networkapp.savings.domain.model.SavingsPlan

/**
 * Changes to the current fixed expenses.
 *
 * @param replacements existing expense id → its new monthly cost
 * @param addedMonthly monthly costs of brand new expenses (new rent with no current one, car loan…)
 * @param addedAccountIds accounts that pay the new expenses; empty = shared by everyone
 * @param oneOffCost one-time costs (deposit, moving, down payment)
 */
data class ScenarioChanges(
    val replacements: Map<Long, Double> = emptyMap(),
    val addedMonthly: List<Double> = emptyList(),
    val addedAccountIds: List<Long> = emptyList(),
    val oneOffCost: Double = 0.0,
)

enum class Affordability {
    /** At least [ScenarioResult.COMFORTABLE_RATE] of the salaries is still saved. */
    COMFORTABLE,
    /** Still saving, but little. */
    TIGHT,
    /** Fixed expenses would exceed the salaries. */
    NOT_AFFORDABLE,
    /** No salaries set, so there is nothing to compare with. */
    UNKNOWN,
}

data class ScenarioResult(
    val before: SavingsPlan,
    val after: SavingsPlan,
    val oneOffCost: Double,
) {
    val monthlyExpenseChange: Double get() = after.totalFixedExpenses - before.totalFixedExpenses

    val affordability: Affordability
        get() {
            val rate = after.savingsRate ?: return Affordability.UNKNOWN
            return when {
                rate < 0.0               -> Affordability.NOT_AFFORDABLE
                rate < COMFORTABLE_RATE  -> Affordability.TIGHT
                else                     -> Affordability.COMFORTABLE
            }
        }

    /** Months of the new savings needed to cover [oneOffCost]; null when nothing is saved. */
    val monthsToCoverOneOff: Double?
        get() = if (oneOffCost <= 0.0 || after.totalSavings <= 0.0) null else oneOffCost / after.totalSavings

    companion object {
        const val COMFORTABLE_RATE = 0.20
    }
}
