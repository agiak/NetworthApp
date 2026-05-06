package com.agcoding.networkapp.home.data.mapper

import com.agcoding.networkapp.home.data.local.NetWorthEntity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class NetWorthEntityToDomainMapperTest {

    private lateinit var mapper: NetWorthEntityToDomainMapper

    @Before
    fun setUp() {
        mapper = NetWorthEntityToDomainMapper()
    }

    @Test
    fun `map entity to domain correctly`() {
        val date = LocalDate.of(2026, 3, 15)
        val entity = NetWorthEntity(id = 1L, value = 42500.0, dateEpochDay = date.toEpochDay())
        val result = mapper.map(entity)
        assertEquals(1L, result.id)
        assertEquals(42500.0, result.value, 0.0)
        assertEquals(date, result.date)
    }

    @Test
    fun `map entity with zero value`() {
        val entity = NetWorthEntity(id = 0L, value = 0.0, dateEpochDay = LocalDate.now().toEpochDay())
        val result = mapper.map(entity)
        assertEquals(0.0, result.value, 0.0)
    }

    @Test
    fun `map entity with negative value`() {
        val date = LocalDate.of(2026, 1, 1)
        val entity = NetWorthEntity(id = 2L, value = -500.0, dateEpochDay = date.toEpochDay())
        val result = mapper.map(entity)
        assertEquals(-500.0, result.value, 0.0)
        assertEquals(date, result.date)
    }
}
