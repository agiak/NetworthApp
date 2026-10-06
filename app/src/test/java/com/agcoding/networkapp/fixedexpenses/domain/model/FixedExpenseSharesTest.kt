package com.agcoding.networkapp.fixedexpenses.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FixedExpenseSharesTest {

    private val all = listOf(1L, 2L)

    @Test
    fun `own expense is fully carried by its account and not by others`() {
        val car = FixedExpense(title = "Car", cost = 300.0, accountIds = listOf(1L))
        assertEquals(1.0, car.shareFor(setOf(1L), all), 0.0)
        assertEquals(0.0, car.shareFor(setOf(2L), all), 0.0)
    }

    @Test
    fun `shared expense gives each account an equal share`() {
        val rent = FixedExpense(title = "Rent", cost = 800.0, accountIds = listOf(1L, 2L))
        assertEquals(0.5, rent.shareFor(setOf(1L), all), 0.0)
        assertEquals(1.0, rent.shareFor(setOf(1L, 2L), all), 0.0)
    }

    @Test
    fun `unassigned expense is split across all accounts`() {
        val internet = FixedExpense(title = "Internet", cost = 24.0)
        assertEquals(0.5, internet.shareFor(setOf(2L), all), 0.0)
        assertEquals(1.0 / 3.0, internet.shareFor(setOf(2L), listOf(1L, 2L, 3L)), 1e-9)
    }

    @Test
    fun `expense of a deleted account is shared by everyone`() {
        val old = FixedExpense(title = "Old", cost = 10.0, accountIds = listOf(99L))
        assertEquals(listOf(1L, 2L), old.sharingAccountIds(all))
    }

    @Test
    fun `whole expense counts when there are no accounts`() {
        val expense = FixedExpense(title = "X", cost = 10.0)
        assertEquals(1.0, expense.shareFor(emptySet(), emptyList()), 0.0)
    }

    @Test
    fun `annual expense monthly cost is a twelfth`() {
        val gym = FixedExpense(title = "Gym", cost = 600.0, recurrence = RecurrenceType.ANNUAL)
        assertEquals(50.0, gym.monthlyCost(), 0.0)
    }
}
