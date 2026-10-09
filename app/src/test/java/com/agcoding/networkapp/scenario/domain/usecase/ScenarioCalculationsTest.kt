package com.agcoding.networkapp.scenario.domain.usecase

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType.ANNUAL
import com.agcoding.networkapp.scenario.domain.model.Affordability
import com.agcoding.networkapp.scenario.domain.model.HomeExpenseType
import com.agcoding.networkapp.scenario.domain.model.ScenarioChanges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioCalculationsTest {

    private val accounts = listOf(
        Account(1, "Anastasis", monthlySalary = 2_000.0),
        Account(2, "Xristina", monthlySalary = 1_500.0),
    )
    private val expenses = listOf(
        FixedExpense(1, "House", cost = 350.0),
        FixedExpense(8, "Electricity", cost = 75.0),
        FixedExpense(7, "Internet", cost = 24.0),
        FixedExpense(5, "Car insurance", cost = 600.0, recurrence = ANNUAL),
        FixedExpense(3, "Parking", cost = 145.0),
        FixedExpense(2, "Claude", cost = 18.0, accountIds = listOf(1)),
    )

    @Test
    fun `home expenses are recognised by name`() {
        assertEquals(HomeExpenseType.RENT, classifyHomeExpense("House"))
        assertEquals(HomeExpenseType.RENT, classifyHomeExpense("Ενοίκιο"))
        assertEquals(HomeExpenseType.ELECTRICITY, classifyHomeExpense("Ρεύμα σπιτιού"))
        assertEquals(HomeExpenseType.ELECTRICITY, classifyHomeExpense("Electricity"))
        assertEquals(HomeExpenseType.WATER, classifyHomeExpense("Νερό ΕΥΔΑΠ"))
        assertEquals(HomeExpenseType.COMMON_CHARGES, classifyHomeExpense("Κοινόχρηστα"))
        assertEquals(HomeExpenseType.INTERNET, classifyHomeExpense("Internet"))
        assertEquals(HomeExpenseType.HEATING, classifyHomeExpense("Πετρέλαιο θέρμανσης"))
        assertNull(classifyHomeExpense("Car insurance"))
        assertNull(classifyHomeExpense("Gym"))
    }

    @Test
    fun `public transport does not scale with cars`() {
        assertTrue(isPerCarExpense("Car insurance"))
        assertTrue(isPerCarExpense("Βενζίνη"))
        assertFalse(isPerCarExpense("Κάρτα μετρό"))
        assertFalse(isPerCarExpense("Taxi"))
    }

    @Test
    fun `adding a car raises its costs by the chosen share`() {
        assertEquals(150.0, scaleCarCost(100.0, currentCars = 1, newCars = 2, increasePerCar = 0.5), 1e-9)
        assertEquals(220.0, scaleCarCost(100.0, currentCars = 1, newCars = 3, increasePerCar = 0.6), 1e-9)
        assertEquals(50.0, scaleCarCost(100.0, currentCars = 2, newCars = 1, increasePerCar = 0.5), 1e-9)
        assertEquals(0.0, scaleCarCost(100.0, currentCars = 1, newCars = 0, increasePerCar = 1.0), 1e-9)
    }

    @Test
    fun `new total is split in proportion to current costs`() {
        val parts = distribute(200.0, listOf(FixedExpense(1, "A", cost = 30.0), FixedExpense(2, "B", cost = 10.0)))
        assertEquals(150.0, parts[1]!!, 1e-9)
        assertEquals(50.0, parts[2]!!, 1e-9)
    }

    @Test
    fun `moving and a second car`() {
        val changes = ScenarioChanges(
            replacements = mapOf(
                1L to 700.0,                                   // new rent
                8L to 90.0,                                    // electricity
                5L to scaleCarCost(50.0, 1, 2, 0.5),           // insurance 600/yr → 75/mo
                3L to scaleCarCost(145.0, 1, 2, 0.5),          // parking → 217.5
            ),
            addedMonthly = listOf(60.0, 250.0),                // common charges, car loan
            oneOffCost = 2_100.0,
        )
        val result = evaluateScenario(accounts, expenses, changes)
        assertEquals(662.0, result.before.totalFixedExpenses, 1e-9)
        assertEquals(1_434.5, result.after.totalFixedExpenses, 1e-9)
        assertEquals(772.5, result.monthlyExpenseChange, 1e-9)
        assertEquals(2_065.5, result.after.totalSavings, 1e-9)
        assertEquals(Affordability.COMFORTABLE, result.affordability)
        assertEquals(2_100.0 / 2_065.5, result.monthsToCoverOneOff!!, 1e-9)
        // New shared expenses split between both: 1434.5 = 18 (Claude) + 1416.5 shared
        assertEquals(18.0 + 1_416.5 / 2, result.after.accounts[0].monthlyFixedExpenses, 1e-9)
    }

    @Test
    fun `new expenses can go to one account`() {
        val result = evaluateScenario(accounts, expenses, ScenarioChanges(addedMonthly = listOf(300.0), addedAccountIds = listOf(2L)))
        assertEquals(result.before.accounts[1].monthlyFixedExpenses + 300.0, result.after.accounts[1].monthlyFixedExpenses, 1e-9)
        assertEquals(result.before.accounts[0].monthlyFixedExpenses, result.after.accounts[0].monthlyFixedExpenses, 1e-9)
    }

    @Test
    fun `affordability follows the savings rate`() {
        assertEquals(Affordability.TIGHT, evaluateScenario(accounts, expenses, ScenarioChanges(addedMonthly = listOf(2_500.0))).affordability)
        assertEquals(Affordability.NOT_AFFORDABLE, evaluateScenario(accounts, expenses, ScenarioChanges(addedMonthly = listOf(4_000.0))).affordability)
        val noSalary = accounts.map { it.copy(monthlySalary = 0.0) }
        assertEquals(Affordability.UNKNOWN, evaluateScenario(noSalary, expenses, ScenarioChanges()).affordability)
    }
}
