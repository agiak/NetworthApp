package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddNetWorthEntryUseCaseImplTest {

    private lateinit var repository: NetWorthRepository
    private lateinit var useCase: AddNetWorthEntryUseCaseImpl

    @Before
    fun setUp() {
        repository = mockk()
        useCase = AddNetWorthEntryUseCaseImpl(repository)
    }

    @Test
    fun `invoke delegates to repository and returns success`() = runTest {
        val entry = NetWorthEntry(value = 42500.0, timestamp = 1000L)
        coEvery { repository.addEntry(entry) } returns Result.success(Unit)
        val result = useCase(entry)
        assertTrue(result.isSuccess)
        coVerify { repository.addEntry(entry) }
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        val entry = NetWorthEntry(value = 42500.0, timestamp = 1000L)
        coEvery { repository.addEntry(entry) } returns Result.failure(RuntimeException("error"))
        val result = useCase(entry)
        assertTrue(result.isFailure)
    }
}
