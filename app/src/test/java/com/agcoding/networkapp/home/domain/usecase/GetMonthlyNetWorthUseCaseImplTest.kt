package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GetMonthlyNetWorthUseCaseImplTest {

    private lateinit var repository: NetWorthRepository
    private lateinit var useCase: GetMonthlyNetWorthUseCaseImpl

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetMonthlyNetWorthUseCaseImpl(repository)
    }

    @Test
    fun `single entry produces one monthly value`() = runTest {
        val entries = listOf(NetWorthEntry(1L, 42500.0, LocalDate.of(2026, 3, 15)))
        every { repository.getEntries() } returns flowOf(Result.success(entries))
        val results = useCase().toList()
        val monthly = results.first().getOrThrow()
        assertEquals(1, monthly.size)
        assertEquals(42500.0, monthly.first().value, 0.0)
        assertEquals(YearMonth.of(2026, 3), monthly.first().yearMonth)
    }

    @Test
    fun `multiple entries in same month uses latest`() = runTest {
        val entries = listOf(
            NetWorthEntry(1L, 1000.0, LocalDate.of(2026, 3, 5)),
            NetWorthEntry(2L, 1200.0, LocalDate.of(2026, 3, 20))
        )
        every { repository.getEntries() } returns flowOf(Result.success(entries))
        val results = useCase().toList()
        val monthly = results.first().getOrThrow()
        assertEquals(1, monthly.size)
        assertEquals(1200.0, monthly.first().value, 0.0)
        assertFalse(monthly.first().isCarriedForward)
    }

    @Test
    fun `missing month is carried forward from previous`() = runTest {
        val entries = listOf(
            NetWorthEntry(1L, 1000.0, LocalDate.of(2026, 1, 1)),
            NetWorthEntry(2L, 1200.0, LocalDate.of(2026, 3, 1))
        )
        every { repository.getEntries() } returns flowOf(Result.success(entries))
        val results = useCase().toList()
        val monthly = results.first().getOrThrow()
        assertEquals(3, monthly.size)
        assertEquals(1000.0, monthly[1].value, 0.0)
        assertTrue(monthly[1].isCarriedForward)
        assertEquals(YearMonth.of(2026, 2), monthly[1].yearMonth)
    }

    @Test
    fun `empty entries returns empty list`() = runTest {
        every { repository.getEntries() } returns flowOf(Result.success(emptyList()))
        val results = useCase().toList()
        assertTrue(results.first().getOrThrow().isEmpty())
    }

    @Test
    fun `propagates repository failure`() = runTest {
        every { repository.getEntries() } returns flowOf(Result.failure(RuntimeException("error")))
        val results = useCase().toList()
        assertTrue(results.first().isFailure)
    }
}
