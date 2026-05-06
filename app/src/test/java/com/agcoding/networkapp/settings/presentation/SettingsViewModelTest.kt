package com.agcoding.networkapp.settings.presentation

import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.usecase.GenerateDummyDataUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetAppLanguageUseCase
import com.agcoding.networkapp.settings.domain.usecase.GetAppThemeUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetAppLanguageUseCase
import com.agcoding.networkapp.settings.domain.usecase.SetAppThemeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var getAppThemeUseCase: GetAppThemeUseCase
    private lateinit var getAppLanguageUseCase: GetAppLanguageUseCase
    private lateinit var setAppThemeUseCase: SetAppThemeUseCase
    private lateinit var setAppLanguageUseCase: SetAppLanguageUseCase
    private lateinit var generateDummyDataUseCase: GenerateDummyDataUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getAppThemeUseCase = mockk()
        getAppLanguageUseCase = mockk()
        setAppThemeUseCase = mockk(relaxed = true)
        setAppLanguageUseCase = mockk(relaxed = true)
        generateDummyDataUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(
        getAppThemeUseCase = getAppThemeUseCase,
        getAppLanguageUseCase = getAppLanguageUseCase,
        setAppThemeUseCase = setAppThemeUseCase,
        setAppLanguageUseCase = setAppLanguageUseCase,
        generateDummyDataUseCase = generateDummyDataUseCase,
        ioDispatcher = testDispatcher
    )

    @Test
    fun `initial state reflects stored theme and language`() = runTest {
        every { getAppThemeUseCase() } returns flowOf(AppTheme.DARK)
        every { getAppLanguageUseCase() } returns flowOf(AppLanguage.GREEK)
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AppTheme.DARK, vm.uiState.value.appTheme)
        assertEquals(AppLanguage.GREEK, vm.uiState.value.appLanguage)
    }

    @Test
    fun `SetTheme calls setAppThemeUseCase`() = runTest {
        every { getAppThemeUseCase() } returns flowOf(AppTheme.SYSTEM)
        every { getAppLanguageUseCase() } returns flowOf(AppLanguage.ENGLISH)
        val vm = createViewModel()
        vm.onIntent(SettingsIntent.SetTheme(AppTheme.LIGHT))
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { setAppThemeUseCase(AppTheme.LIGHT) }
    }

    @Test
    fun `SetLanguage calls setAppLanguageUseCase`() = runTest {
        every { getAppThemeUseCase() } returns flowOf(AppTheme.SYSTEM)
        every { getAppLanguageUseCase() } returns flowOf(AppLanguage.ENGLISH)
        val vm = createViewModel()
        vm.onIntent(SettingsIntent.SetLanguage(AppLanguage.GREEK))
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { setAppLanguageUseCase(AppLanguage.GREEK) }
    }

    @Test
    fun `GenerateDummyData sets Success result`() = runTest {
        every { getAppThemeUseCase() } returns flowOf(AppTheme.SYSTEM)
        every { getAppLanguageUseCase() } returns flowOf(AppLanguage.ENGLISH)
        coEvery { generateDummyDataUseCase() } returns Result.success(Unit)
        val vm = createViewModel()
        vm.onIntent(SettingsIntent.GenerateDummyData)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isDummyDataGenerating)
        assertTrue(vm.uiState.value.dummyDataResult is DummyDataResult.Success)
    }

    @Test
    fun `GenerateDummyData sets Failure result on error`() = runTest {
        every { getAppThemeUseCase() } returns flowOf(AppTheme.SYSTEM)
        every { getAppLanguageUseCase() } returns flowOf(AppLanguage.ENGLISH)
        coEvery { generateDummyDataUseCase() } returns Result.failure(RuntimeException("DB error"))
        val vm = createViewModel()
        vm.onIntent(SettingsIntent.GenerateDummyData)
        testDispatcher.scheduler.advanceUntilIdle()
        val result = vm.uiState.value.dummyDataResult
        assertTrue(result is DummyDataResult.Failure)
        assertEquals("DB error", (result as DummyDataResult.Failure).cause)
    }

    @Test
    fun `ClearDummyDataResult resets result to null`() = runTest {
        every { getAppThemeUseCase() } returns flowOf(AppTheme.SYSTEM)
        every { getAppLanguageUseCase() } returns flowOf(AppLanguage.ENGLISH)
        coEvery { generateDummyDataUseCase() } returns Result.success(Unit)
        val vm = createViewModel()
        vm.onIntent(SettingsIntent.GenerateDummyData)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onIntent(SettingsIntent.ClearDummyDataResult)
        assertNull(vm.uiState.value.dummyDataResult)
    }
}
