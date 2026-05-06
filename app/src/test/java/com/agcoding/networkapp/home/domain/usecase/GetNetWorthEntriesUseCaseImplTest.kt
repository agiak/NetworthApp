package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetNetWorthEntriesUseCaseImplTest {

    private lateinit var repository: NetWorthRepository
    private lateinit var useCase: GetNetWorthEntriesUseCaseImpl

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetNetWorthEntriesUseCaseImpl(repository)
    }

    @Test
    fun `invoke returns flow from repository`() = runTest {
        val entries = listOf(NetWorthEntry(1L, 42500.0, 1000L))
        every { repository.getEntries() } returns flowOf(Result.success(entries))
        val results = useCase().toList()
        assertTrue(results.first().isSuccess)
        assertEquals(42500.0, results.first().getOrThrow().first().value, 0.0)
        verify { repository.getEntries() }
    }

    @Test
    fun `invoke returns empty list when no entries`() = runTest {
        every { repository.getEntries() } returns flowOf(Result.success(emptyList()))
        val results = useCase().toList()
        assertTrue(results.first().isSuccess)
        assertTrue(results.first().getOrThrow().isEmpty())
    }

    @Test
    fun `invoke propagates failure from repository`() = runTest {
        every { repository.getEntries() } returns flowOf(Result.failure(RuntimeException("error")))
        val results = useCase().toList()
        assertTrue(results.first().isFailure)
    }
}
