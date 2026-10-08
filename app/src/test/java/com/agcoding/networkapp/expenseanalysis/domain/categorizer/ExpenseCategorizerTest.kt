package com.agcoding.networkapp.expenseanalysis.domain.categorizer

import com.agcoding.networkapp.expenseanalysis.domain.model.CategoryFeedback
import com.agcoding.networkapp.expenseanalysis.domain.model.CategorySource
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.DINING_LEISURE
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.FINANCE
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.GROCERIES
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.HEALTH
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.HOUSING
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.OTHER
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.SUBSCRIPTIONS
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.TRANSPORT
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory.TRAVEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCategorizerTest {

    private val categorizer = ExpenseCategorizer()

    private fun assertCategory(expected: ExpenseCategory, title: String, c: ExpenseCategorizer = categorizer) =
        assertEquals("\"$title\"", expected, c.categorize(title).category)

    @Test
    fun `english names`() {
        assertCategory(FINANCE, "Bank fees")
        assertCategory(SUBSCRIPTIONS, "Revolut plus")
        assertCategory(SUBSCRIPTIONS, "Gym")
        assertCategory(HOUSING, "Electricity")
        assertCategory(HOUSING, "Internet")
        assertCategory(HOUSING, "Cell phone bill")
        assertCategory(TRANSPORT, "Car insurance")
        assertCategory(SUBSCRIPTIONS, "Google One")
        assertCategory(TRANSPORT, "Parking")
        assertCategory(SUBSCRIPTIONS, "Claude")
        assertCategory(HOUSING, "House")
        assertCategory(SUBSCRIPTIONS, "Netflix")
        assertCategory(SUBSCRIPTIONS, "ChatGPT Plus")
        assertCategory(TRAVEL, "Hotel Paris")
        assertCategory(TRAVEL, "Rent a car")
    }

    @Test
    fun `greek names with or without accents`() {
        assertCategory(HOUSING, "Ρεύμα")
        assertCategory(HOUSING, "ΡΕΥΜΑ ΔΕΗ")
        assertCategory(HOUSING, "Νερό ΕΥΔΑΠ")
        assertCategory(HOUSING, "Κοινόχρηστα")
        assertCategory(HOUSING, "Ενοίκιο")
        assertCategory(TRANSPORT, "Βενζίνη")
        assertCategory(TRANSPORT, "Ασφάλεια αυτοκινήτου")
        assertCategory(TRANSPORT, "Σέρβις αυτοκινήτου")
        assertCategory(TRANSPORT, "Ταξί")
        assertCategory(TRAVEL, "Ταξίδι Ιταλία")
        assertCategory(TRAVEL, "Ξενοδοχείο")
        assertCategory(SUBSCRIPTIONS, "Γυμναστήριο")
        assertCategory(SUBSCRIPTIONS, "Σπότιφαϊ")
        assertCategory(DINING_LEISURE, "Φαγητό έξω")
        assertCategory(DINING_LEISURE, "Καφές")
        assertCategory(GROCERIES, "Σούπερ μάρκετ")
        assertCategory(HEALTH, "Ασφάλεια υγείας")
        assertCategory(HEALTH, "Φαρμακείο")
        assertCategory(FINANCE, "Δόση δανείου")
    }

    @Test
    fun `greeklish and typos`() {
        assertCategory(TRANSPORT, "benzinh")
        assertCategory(HOUSING, "revma")
        assertCategory(SUBSCRIPTIONS, "Netflx")
        assertCategory(HOUSING, "Electricty")
    }

    @Test
    fun `unknown name falls back to other`() {
        val prediction = categorizer.categorize("Xyzzy")
        assertEquals(OTHER, prediction.category)
        assertEquals(CategorySource.NONE, prediction.source)
        assertTrue(prediction.isUncertain)
    }

    @Test
    fun `manual choice wins for the same name`() {
        val c = ExpenseCategorizer(listOf(CategoryFeedback(TextNormalizer.key("Gym"), HEALTH)))
        val prediction = c.categorize("  gym ")
        assertEquals(HEALTH, prediction.category)
        assertEquals(CategorySource.MANUAL, prediction.source)
    }

    @Test
    fun `learns words from the user's choices`() {
        assertCategory(OTHER, "Μαρία σκάλα")
        val c = ExpenseCategorizer(listOf(CategoryFeedback(TextNormalizer.key("Μαρία"), HOUSING)))
        val prediction = c.categorize("Μαρία σκάλα")
        assertEquals(HOUSING, prediction.category)
        assertEquals(CategorySource.LEARNED, prediction.source)
    }

    @Test
    fun `repeated choices overrule the dictionary`() {
        // "pro" hints at a subscription, but the user files Figma under banking & taxes (work costs)
        assertCategory(SUBSCRIPTIONS, "Figma pro")
        val c = ExpenseCategorizer(listOf(
            CategoryFeedback("figma", FINANCE),
            CategoryFeedback("figma team", FINANCE),
        ))
        val prediction = c.categorize("Figma pro")
        assertEquals(FINANCE, prediction.category)
        assertEquals(CategorySource.LEARNED, prediction.source)
    }

    @Test
    fun `key ignores case accents and punctuation`() {
        assertEquals(TextNormalizer.key("Ρεύμα - ΔΕΗ!"), TextNormalizer.key("ρευμα δεη"))
        assertEquals("revma", TextNormalizer.key("Ρεύμα"))
        assertEquals("netflix", TextNormalizer.key("Νέτφλιξ"))
        assertEquals("spotifai", TextNormalizer.key("Σπότιφαϊ"))
    }

    @Test
    fun `note adds weaker evidence`() {
        assertEquals(TRAVEL, categorizer.categorize("Χρήστος", note = "ταξίδι").category)
    }
}
