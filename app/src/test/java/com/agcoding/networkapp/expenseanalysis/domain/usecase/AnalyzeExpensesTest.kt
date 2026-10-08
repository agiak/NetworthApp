package com.agcoding.networkapp.expenseanalysis.domain.usecase

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.expenseanalysis.domain.categorizer.ExpenseCategorizer
import com.agcoding.networkapp.expenseanalysis.domain.model.CategoryFeedback
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseAnalysis
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType.ANNUAL
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType.MONTHLY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeExpensesTest {

    private val accounts = listOf(Account(1, "Anastasis"), Account(2, "Xristina"))

    private val expenses = listOf(
        FixedExpense(11, "Bank fees", cost = 8.5, accountIds = listOf(1)),
        FixedExpense(10, "Revolut plus", cost = 4.0, accountIds = listOf(1)),
        FixedExpense(9, "Gym", cost = 580.0, recurrence = ANNUAL, accountIds = listOf(1)),
        FixedExpense(8, "Electricity", cost = 75.0),
        FixedExpense(7, "Internet", cost = 24.0),
        FixedExpense(6, "Cell phone bill", cost = 12.0, accountIds = listOf(1)),
        FixedExpense(5, "Car insurance", cost = 600.0, recurrence = ANNUAL),
        FixedExpense(4, "Google One", cost = 22.0, recurrence = ANNUAL, accountIds = listOf(1)),
        FixedExpense(3, "Parking", cost = 145.0, recurrence = MONTHLY),
        FixedExpense(2, "Claude", cost = 18.0, accountIds = listOf(1)),
        FixedExpense(1, "House", cost = 350.0),
    )

    private fun analyze(accountId: Long?, categorizer: ExpenseCategorizer = ExpenseCategorizer()) =
        analyzeExpenses(expenses, accounts, accountId, categorizer)

    private fun ExpenseAnalysis.total(category: ExpenseCategory) =
        categories.find { it.category == category }?.monthlyTotal ?: 0.0

    @Test
    fun `all accounts together`() {
        val analysis = analyze(null)
        assertEquals(736.67, analysis.monthlyTotal, 0.01)
        assertEquals(461.0, analysis.total(ExpenseCategory.HOUSING), 0.01)        // house, power, internet, phone
        assertEquals(195.0, analysis.total(ExpenseCategory.TRANSPORT), 0.01)      // parking + 600/12
        assertEquals(72.17, analysis.total(ExpenseCategory.SUBSCRIPTIONS), 0.01)  // gym/12, revolut, google/12, claude
        assertEquals(8.5, analysis.total(ExpenseCategory.FINANCE), 0.01)
        assertEquals(1.0, analysis.categories.sumOf { it.shareOfTotal }, 1e-9)
        // Largest first
        assertEquals(ExpenseCategory.HOUSING, analysis.categories.first().category)
    }

    @Test
    fun `single account counts only its share of shared expenses`() {
        val anastasis = analyze(1)
        val xristina = analyze(2)
        assertEquals(414.67, anastasis.monthlyTotal, 0.01)
        assertEquals(322.0, xristina.monthlyTotal, 0.01)
        // Her half of house 175 + electricity 37.5 + internet 12
        assertEquals(224.5, xristina.total(ExpenseCategory.HOUSING), 0.01)
        assertEquals(0.0, xristina.total(ExpenseCategory.SUBSCRIPTIONS), 0.01)
        assertTrue(xristina.categories.none { it.category == ExpenseCategory.SUBSCRIPTIONS })
    }

    @Test
    fun `per account amounts add up to the category total`() {
        val analysis = analyze(null)
        analysis.categories.forEach { category ->
            assertEquals(category.monthlyTotal, category.perAccount.sumOf { it.monthlyAmount }, 1e-9)
        }
    }

    @Test
    fun `user choice moves an expense to another category`() {
        val categorizer = ExpenseCategorizer(listOf(CategoryFeedback("parking", ExpenseCategory.HOUSING)))
        val analysis = analyze(null, categorizer)
        assertEquals(50.0, analysis.total(ExpenseCategory.TRANSPORT), 0.01)
        assertEquals(606.0, analysis.total(ExpenseCategory.HOUSING), 0.01)
    }
}
