package com.agcoding.networkapp.scenario.domain.usecase

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.expenseanalysis.domain.categorizer.TextNormalizer
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.domain.model.monthlyCost
import com.agcoding.networkapp.savings.domain.usecase.computeSavingsPlan
import com.agcoding.networkapp.scenario.domain.model.HomeExpenseType
import com.agcoding.networkapp.scenario.domain.model.ScenarioChanges
import com.agcoding.networkapp.scenario.domain.model.ScenarioResult
import kotlin.math.max

/**
 * Word stems that identify each home cost, checked in this order so that
 * "Ρεύμα σπιτιού" is electricity and not rent.
 */
private val HOME_STEMS: List<Pair<HomeExpenseType, List<String>>> = listOf(
    HomeExpenseType.ELECTRICITY    to listOf("ρευμα", "ηλεκτρικ", "ηλεκτρισμ", "electric", "power", "δεη", "protergia", "heron", "elpedison", "nrg"),
    HomeExpenseType.WATER          to listOf("νερο", "νερου", "water", "ευδαπ", "ευαθ", "δευα", "υδρευσ"),
    HomeExpenseType.COMMON_CHARGES to listOf("κοινοχρηστ", "common", "building", "πολυκατοικ"),
    HomeExpenseType.INTERNET       to listOf("internet", "ιντερνετ", "wifi", "broadband", "fiber", "inalan"),
    HomeExpenseType.HEATING        to listOf("θερμανσ", "heating", "αεριο", "natural gas", "zenith", "πετρελαιο θερμανσης"),
    HomeExpenseType.RENT           to listOf("ενοικ", "rent", "house", "σπιτ", "home", "apartment", "διαμερισμ", "κατοικ", "mortgage", "στεγαστικ"),
).map { (type, words) -> type to words.map { TextNormalizer.key(it) } }

/** Words of expenses that don't grow with the number of cars (public transport, taxis). */
private val NOT_PER_CAR = listOf(
    "μετρο", "metro", "λεωφορει", "bus", "οασα", "κτελ", "ktel", "τρενο", "train", "ταξι", "taxi", "uber",
    "bolt", "beat", "freenow", "εισιτηρι", "ticket", "πατινι", "scooter", "lime",
).map { TextNormalizer.key(it) }.toSet()

/** Which home cost [title] is, or null if it isn't one of them. */
fun classifyHomeExpense(title: String): HomeExpenseType? {
    val tokens = TextNormalizer.tokens(title)
    if (tokens.isEmpty()) return null
    val joined = tokens.joinToString(" ")
    return HOME_STEMS.firstOrNull { (_, stems) ->
        stems.any { stem -> if (' ' in stem) stem in joined else tokens.any { it.startsWith(stem) } }
    }?.first
}

/** Transport expenses that belong to owning a car (fuel, insurance, service, parking…). */
fun isPerCarExpense(title: String): Boolean =
    TextNormalizer.tokens(title).none { token -> NOT_PER_CAR.any { token.startsWith(it) } }

/**
 * Cost of a car expense after changing the number of cars: each extra car adds
 * [increasePerCar] (0.5 = +50%) of today's cost, each car less removes it, never below zero.
 */
fun scaleCarCost(currentMonthly: Double, currentCars: Int, newCars: Int, increasePerCar: Double): Double {
    if (currentCars <= 0) return currentMonthly
    return currentMonthly * max(0.0, 1.0 + increasePerCar * (newCars - currentCars))
}

/** Splits [total] over [expenses] in proportion to their current monthly cost (evenly if all are zero). */
fun distribute(total: Double, expenses: List<FixedExpense>): Map<Long, Double> {
    if (expenses.isEmpty()) return emptyMap()
    val currentTotal = expenses.sumOf { it.monthlyCost() }
    return expenses.associate { expense ->
        val weight = if (currentTotal > 0.0) expense.monthlyCost() / currentTotal else 1.0 / expenses.size
        expense.id to total * weight
    }
}

/** The fixed expenses as they would be after [changes]. */
fun applyScenario(expenses: List<FixedExpense>, changes: ScenarioChanges): List<FixedExpense> {
    val changed = expenses.map { expense ->
        val newCost = changes.replacements[expense.id] ?: return@map expense
        expense.copy(cost = newCost.coerceAtLeast(0.0), recurrence = RecurrenceType.MONTHLY)
    }
    val nextId = (expenses.maxOfOrNull { it.id } ?: 0L) + 1L
    val added = changes.addedMonthly
        .filter { it > 0.0 }
        .mapIndexed { index, cost ->
            FixedExpense(id = nextId + index, title = "", cost = cost, accountIds = changes.addedAccountIds)
        }
    return changed + added
}

fun evaluateScenario(accounts: List<Account>, expenses: List<FixedExpense>, changes: ScenarioChanges) = ScenarioResult(
    before     = computeSavingsPlan(accounts, expenses),
    after      = computeSavingsPlan(accounts, applyScenario(expenses, changes)),
    oneOffCost = changes.oneOffCost.coerceAtLeast(0.0),
)
