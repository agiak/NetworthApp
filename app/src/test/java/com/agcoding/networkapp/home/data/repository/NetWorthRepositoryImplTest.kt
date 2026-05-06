package com.agcoding.networkapp.home.data.repository

import com.agcoding.networkapp.home.data.local.NetWorthDao
import com.agcoding.networkapp.home.data.local.NetWorthEntity
import com.agcoding.networkapp.home.data.mapper.NetWorthEntityToDomainMapper
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class NetWorthRepositoryImplTest {

    private lateinit var dao: NetWorthDao
    private lateinit var mapper: NetWorthEntityToDomainMapper
    private lateinit var repository: NetWorthRepositoryImpl
    private val testDate = LocalDate.of(2026, 3, 15)

    @Before
    fun setUp() {
        dao = mockk()
        mapper = NetWorthEntityToDomainMapper()
        repository = NetWorthRepositoryImpl(dao, mapper, UnconfinedTestDispatcher())
    }

    @Test
    fun `getEntries emits mapped entries on success`() = runTest {
        val entities = listOf(NetWorthEntity(1L, 42500.0, testDate.toEpochDay()))
        every { dao.getAllEntries() } returns flowOf(entities)
        val results = repository.getEntries().toList()
        assertTrue(results.first().isSuccess)
        assertEquals(42500.0, results.first().getOrThrow().first().value, 0.0)
        assertEquals(testDate, results.first().getOrThrow().first().date)
    }

    @Test
    fun `getEntries emits empty list when no entries`() = runTest {
        every { dao.getAllEntries() } returns flowOf(emptyList())
        val results = repository.getEntries().toList()
        assertTrue(results.first().isSuccess)
        assertTrue(results.first().getOrThrow().isEmpty())
    }

    @Test
    fun `addEntry calls dao and returns success`() = runTest {
        coEvery { dao.insertEntry(any()) } returns Unit
        val entry = NetWorthEntry(value = 42500.0, date = testDate)
        val result = repository.addEntry(entry)
        assertTrue(result.isSuccess)
        coVerify { dao.insertEntry(match { it.dateEpochDay == testDate.toEpochDay() }) }
    }

    @Test
    fun `addEntry returns failure when dao throws`() = runTest {
        coEvery { dao.insertEntry(any()) } throws RuntimeException("DB error")
        val entry = NetWorthEntry(value = 42500.0, date = testDate)
        val result = repository.addEntry(entry)
        assertTrue(result.isFailure)
    }
}
