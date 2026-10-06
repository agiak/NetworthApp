package com.agcoding.networkapp.savings.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectSavingsTest {

    @Test
    fun `without return the total is just the deposits`() {
        val projection = projectSavings(monthlyAmount = 500.0, years = 3)
        assertEquals(18_000.0, projection.total, 0.001)
        assertEquals(18_000.0, projection.deposits, 0.001)
        assertEquals(0.0, projection.returns, 0.001)
    }

    @Test
    fun `annual return compounds monthly`() {
        val projection = projectSavings(monthlyAmount = 1_000.0, years = 10, annualReturnPercent = 5.0)
        assertEquals(120_000.0, projection.deposits, 0.001)
        assertEquals(155_282.27, projection.total, 0.01)
        assertEquals(35_282.27, projection.returns, 0.01)
    }

    @Test
    fun `zero years collects nothing`() {
        assertEquals(0.0, projectSavings(monthlyAmount = 1_000.0, years = 0, annualReturnPercent = 5.0).total, 0.0)
    }
}
