package com.agcoding.networkapp.home.presentation

import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.usecase.AddNetWorthEntryUseCase
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCase
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntriesUseCase
import com.agcoding.networkapp.home.presentation.mapper.NetWorthDomainToUiMapper
import com.agcoding.networkapp.home.presentation.mapper.NetWorthEntryToUiMapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var getMonthlyNetWorthUseCase: GetMonthlyNetWorthUseCase
    private lateinit var getNetWorthEntriesUseCase: GetNetWorthEntriesUseCase
    private lateinit var addNetWorthEntryUseCase: AddNetWorthEntryUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getMonthlyNetWorthUseCase = mockk()
        getNetWorthEntriesUseCase = mockk {
            every { invoke() } returns flowOf(Result.success(emptyList()))
        }
        addNetWorthEntryUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(
        getMonthlyNetWorthUseCase = getMonthlyNetWorthUseCase,
        getNetWorthEntriesUseCase = getNetWorthEntriesUseCase,
        addNetWorthEntryUseCase = addNetWorthEntryUseCase,
        mapper = NetWorthDomainToUiMapper(),
        entryMapper = NetWorthEntryToUiMapper(),
        ioDispatcher = testDispatcher
    )

    private fun singleMonthData(value: Double = 42500.0) = listOf(
        MonthlyNetWorth(
            yearMonth = YearMonth.now(),
            value = value,
            lastUpdatedDate = LocalDate.now()
        )
    )

    @Test
    fun `initial state is loading`() {
        every { getMonthlyNetWorthUseCase() } returns flowOf(Result.success(emptyList()))
        val vm = createViewModel()
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `LoadData updates net worth from monthly data`() = runTest {
        every { getMonthlyNetWorthUseCase() } returns flowOf(Result.success(singleMonthData()))
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("€42,500", vm.uiState.value.currentNetWorth)
    }

    @Test
    fun `ShowAddEntrySheet sets isAddEntrySheetVisible true`() = runTest {
        every { getMonthlyNetWorthUseCase() } returns flowOf(Result.success(emptyList()))
        val vm = createViewModel()
        vm.onIntent(HomeIntent.ShowAddEntrySheet)
        assertTrue(vm.uiState.value.isAddEntrySheetVisible)
    }

    @Test
    fun `HideAddEntrySheet hides sheet and resets input and date`() = runTest {
        every { getMonthlyNetWorthUseCase() } returns flowOf(Result.success(emptyList()))
        val vm = createViewModel()
        vm.onIntent(HomeIntent.ShowAddEntrySheet)
        vm.onIntent(HomeIntent.UpdateEntryInput("1000"))
        vm.onIntent(HomeIntent.HideAddEntrySheet)
        assertFalse(vm.uiState.value.isAddEntrySheetVisible)
        assertTrue(vm.uiState.value.entryInput.isEmpty())
    }

    @Test
    fun `UpdateEntryDate updates selectedDate`() = runTest {
        every { getMonthlyNetWorthUseCase() } returns flowOf(Result.success(emptyList()))
        val vm = createViewModel()
        val newDate = LocalDate.of(2026, 3, 20)
        vm.onIntent(HomeIntent.UpdateEntryDate(newDate))
        assertEquals(newDate, vm.uiState.value.selectedDate)
    }

    @Test
    fun `SaveEntry uses selectedDate from state`() = runTest {
        every { getMonthlyNetWorthUseCase() } returns flowOf(Result.success(emptyList()))
        val targetDate = LocalDate.of(2026, 3, 20)
        coEvery { addNetWorthEntryUseCase(match { it.date == targetDate }) } returns Result.success(Unit)
        val vm = createViewModel()
        vm.onIntent(HomeIntent.UpdateEntryDate(targetDate))
        vm.onIntent(HomeIntent.UpdateEntryInput("42500"))
        vm.onIntent(HomeIntent.SaveEntry)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isAddEntrySheetVisible)
    }

    @Test
    fun `SaveEntry with invalid input does nothing`() = runTest {
        every { getMonthlyNetWorthUseCase() } returns flowOf(Result.success(emptyList()))
        val vm = createViewModel()
        vm.onIntent(HomeIntent.UpdateEntryInput("not_a_number"))
        vm.onIntent(HomeIntent.SaveEntry)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `LoadData sets error state on failure`() = runTest {
        every { getMonthlyNetWorthUseCase() } returns flowOf(Result.failure(RuntimeException("DB error")))
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("DB error", vm.uiState.value.error)
    }

    @Test
    fun `ClearError resets error to null`() = runTest {
        every { getMonthlyNetWorthUseCase() } returns flowOf(Result.failure(RuntimeException("DB error")))
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onIntent(HomeIntent.ClearError)
        assertNull(vm.uiState.value.error)
    }
}
