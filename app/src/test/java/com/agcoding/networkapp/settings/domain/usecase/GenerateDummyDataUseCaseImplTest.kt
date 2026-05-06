package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GenerateDummyDataUseCaseImplTest {

    private lateinit var netWorthRepository: NetWorthRepository
    private lateinit var useCase: GenerateDummyDataUseCaseImpl

    @Before
    fun setUp() {
        netWorthRepository = mockk()
        useCase = GenerateDummyDataUseCaseImpl(netWorthRepository)
    }

    @Test
    fun `invoke deletes all entries then inserts 12 entries on success`() = runTest {
        coEvery { netWorthRepository.deleteAllEntries() } returns Result.success(Unit)
        coEvery { netWorthRepository.addEntry(any()) } returns Result.success(Unit)
        val result = useCase()
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { netWorthRepository.deleteAllEntries() }
        coVerify(exactly = 12) { netWorthRepository.addEntry(any()) }
    }

    @Test
    fun `invoke returns failure when deleteAllEntries fails`() = runTest {
        coEvery { netWorthRepository.deleteAllEntries() } returns Result.failure(RuntimeException("delete failed"))
        val result = useCase()
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { netWorthRepository.addEntry(any()) }
    }

    @Test
    fun `invoke returns failure when addEntry fails`() = runTest {
        coEvery { netWorthRepository.deleteAllEntries() } returns Result.success(Unit)
        coEvery { netWorthRepository.addEntry(any()) } returns Result.failure(RuntimeException("insert failed"))
        val result = useCase()
        assertTrue(result.isFailure)
    }

    @Test
    fun `entries span 12 consecutive months ending at current month`() = runTest {
        val capturedEntries = mutableListOf<NetWorthEntry>()
        coEvery { netWorthRepository.deleteAllEntries() } returns Result.success(Unit)
        coEvery { netWorthRepository.addEntry(capture(capturedEntries)) } returns Result.success(Unit)
        useCase()
        val months = capturedEntries.map { it.date.withDayOfMonth(1) }.distinct()
        assertTrue(months.size == 12)
    }
}
