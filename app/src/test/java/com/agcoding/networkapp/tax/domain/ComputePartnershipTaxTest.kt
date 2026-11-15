package com.agcoding.networkapp.tax.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputePartnershipTaxTest {

    @Test
    fun `tax is 22 percent of turnover minus expenses`() {
        val result = computePartnershipTax(turnover = 80_000.0, expenses = 30_000.0)
        assertEquals(50_000.0, result.profit, 0.001)
        assertEquals(11_000.0, result.tax, 0.001)
        assertEquals(39_000.0, result.profitAfterTax, 0.001)
        assertFalse(result.isLoss)
    }

    @Test
    fun `tax is rounded to the cent`() {
        val result = computePartnershipTax(turnover = 1_000.05, expenses = 0.0)
        assertEquals(220.01, result.tax, 0.0001)
        assertEquals(780.04, result.profitAfterTax, 0.0001)
    }

    @Test
    fun `a loss pays no tax`() {
        val result = computePartnershipTax(turnover = 10_000.0, expenses = 12_500.0)
        assertEquals(-2_500.0, result.profit, 0.001)
        assertEquals(0.0, result.tax, 0.0)
        assertEquals(-2_500.0, result.profitAfterTax, 0.001)
        assertTrue(result.isLoss)
    }

    @Test
    fun `no profit pays no tax`() {
        val result = computePartnershipTax(turnover = 5_000.0, expenses = 5_000.0)
        assertEquals(0.0, result.tax, 0.0)
        assertFalse(result.isLoss)
    }
}
