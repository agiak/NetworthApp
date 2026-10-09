package com.agcoding.networkapp.savings.domain.usecase

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.savings.domain.model.selection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComputeSavingsPlanTest {

    private val anastasis = Account(id = 1L, name = "Anastasis", monthlySalary = 2_000.0)
    private val xristina  = Account(id = 2L, name = "Xristina", monthlySalary = 1_500.0)

    @Test
    fun `expense assigned to one account is charged only to that account`() {
        val plan = computeSavingsPlan(
            accounts = listOf(anastasis, xristina),
            expenses = listOf(FixedExpense(title = "Car", cost = 300.0, accountIds = listOf(1L))),
        )
        assertEquals(300.0, plan.accounts[0].monthlyFixedExpenses, 0.001)
        assertEquals(0.0, plan.accounts[1].monthlyFixedExpenses, 0.001)
        assertEquals(1_700.0, plan.accounts[0].monthlySavings, 0.001)
        assertEquals(1_500.0, plan.accounts[1].monthlySavings, 0.001)
    }

    @Test
    fun `shared and unassigned expenses are split evenly`() {
        val plan = computeSavingsPlan(
            accounts = listOf(anastasis, xristina),
            expenses = listOf(
                FixedExpense(title = "Rent", cost = 800.0, accountIds = listOf(1L, 2L)),
                FixedExpense(title = "Internet", cost = 40.0),
            ),
        )
        assertEquals(420.0, plan.accounts[0].monthlyFixedExpenses, 0.001)
        assertEquals(420.0, plan.accounts[1].monthlyFixedExpenses, 0.001)
        assertEquals(840.0, plan.totalFixedExpenses, 0.001)
        assertEquals(3_500.0, plan.totalSalary, 0.001)
        assertEquals(2_660.0, plan.totalSavings, 0.001)
        assertEquals(2, plan.accounts[0].expenseCount)
    }

    @Test
    fun `annual expenses count as a twelfth per month`() {
        val plan = computeSavingsPlan(
            accounts = listOf(anastasis),
            expenses = listOf(FixedExpense(title = "Insurance", cost = 1_200.0, recurrence = RecurrenceType.ANNUAL)),
        )
        assertEquals(100.0, plan.accounts[0].monthlyFixedExpenses, 0.001)
        assertEquals(0.95, plan.accounts[0].savingsRate!!, 0.001)
    }

    @Test
    fun `expense pointing to a deleted account is shared by everyone`() {
        val plan = computeSavingsPlan(
            accounts = listOf(anastasis, xristina),
            expenses = listOf(FixedExpense(title = "Old", cost = 100.0, accountIds = listOf(99L))),
        )
        assertEquals(50.0, plan.accounts[0].monthlyFixedExpenses, 0.001)
        assertEquals(50.0, plan.accounts[1].monthlyFixedExpenses, 0.001)
    }

    @Test
    fun `savings rate is null without salary`() {
        val plan = computeSavingsPlan(
            accounts = listOf(Account(id = 3L, name = "Cash")),
            expenses = emptyList(),
        )
        assertNull(plan.accounts[0].savingsRate)
        assertNull(plan.savingsRate)
    }

    @Test
    fun `selection gives one account or everyone together`() {
        val plan = computeSavingsPlan(
            accounts = listOf(anastasis, xristina),
            expenses = listOf(
                FixedExpense(title = "Rent", cost = 800.0),
                FixedExpense(title = "Gym", cost = 600.0, recurrence = RecurrenceType.ANNUAL, accountIds = listOf(1L)),
            ),
        )
        val all = plan.selection(null)
        assertEquals(3_500.0, all.monthlySalary, 0.001)
        assertEquals(850.0, all.monthlyFixedExpenses, 0.001)
        assertEquals(2_650.0, all.monthlySavings, 0.001)

        val mine = plan.selection(1L)
        assertEquals(450.0, mine.monthlyFixedExpenses, 0.001)
        assertEquals(1_550.0, mine.monthlySavings, 0.001)
        assertEquals(emptyList<Account>(), mine.accountsWithoutSalary)
    }

    @Test
    fun `selection lists accounts without a salary`() {
        val plan = computeSavingsPlan(
            accounts = listOf(anastasis, xristina.copy(monthlySalary = 0.0)),
            expenses = listOf(FixedExpense(title = "Rent", cost = 800.0)),
        )
        assertEquals(listOf("Xristina"), plan.selection(null).accountsWithoutSalary.map { it.name })
        assertEquals(false, plan.selection(2L).hasSalary)
        assertEquals(1_200.0, plan.selection(null).monthlySavings, 0.001)
    }
}
