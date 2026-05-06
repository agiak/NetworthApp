package com.agcoding.networkapp.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.usecase.AddNetWorthEntryUseCase
import com.agcoding.networkapp.home.domain.usecase.GetMonthlyNetWorthUseCase
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntriesUseCase
import com.agcoding.networkapp.home.presentation.mapper.NetWorthDomainToUiMapper
import com.agcoding.networkapp.home.presentation.mapper.NetWorthEntryToUiMapper
import com.agcoding.networkapp.settings.domain.usecase.GetUserProfileUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMonthlyNetWorthUseCase: GetMonthlyNetWorthUseCase,
    private val getNetWorthEntriesUseCase: GetNetWorthEntriesUseCase,
    private val addNetWorthEntryUseCase: AddNetWorthEntryUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val mapper: NetWorthDomainToUiMapper,
    private val entryMapper: NetWorthEntryToUiMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var dataJob: Job? = null
    private var recentEntriesJob: Job? = null

    init {
        loadData()
        loadRecentEntries()
        loadUserProfile()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.ClearError -> _uiState.update { it.copy(error = null) }
            HomeIntent.LoadData -> {
                if (dataJob?.isActive != true) loadData()
                if (recentEntriesJob?.isActive != true) loadRecentEntries()
            }
            HomeIntent.ShowAddEntrySheet -> _uiState.update { it.copy(isAddEntrySheetVisible = true) }
            HomeIntent.HideAddEntrySheet -> _uiState.update { it.copy(isAddEntrySheetVisible = false, entryInput = "", selectedDate = LocalDate.now()) }
            is HomeIntent.UpdateEntryInput -> _uiState.update { it.copy(entryInput = intent.value) }
            is HomeIntent.UpdateEntryDate -> _uiState.update { it.copy(selectedDate = intent.date) }
            HomeIntent.SaveEntry -> saveEntry()
        }
    }

    private fun loadData() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            getMonthlyNetWorthUseCase().collect { result ->
                result.fold(
                    onSuccess = { monthlyData ->
                        val displayData = mapper.map(monthlyData)
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                currentNetWorth = displayData.currentNetWorth,
                                changeThisMonth = displayData.changeThisMonth,
                                changePercentage = displayData.changePercentage,
                                isPositiveChange = displayData.isPositiveChange,
                                lastUpdatedDate = displayData.lastUpdatedDate,
                                chartData = displayData.chartData,
                                insights = displayData.insights,
                                ytdGrowth = displayData.ytdGrowth,
                                ytdPercentage = displayData.ytdPercentage,
                                avgPerMonth = displayData.avgPerMonth,
                                streakMonths = displayData.streakMonths,
                                isStreakPositive = displayData.isStreakPositive,
                                error = null
                            )
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error)
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }

    private fun loadRecentEntries() {
        recentEntriesJob?.cancel()
        recentEntriesJob = viewModelScope.launch {
            getNetWorthEntriesUseCase().collect { result ->
                result.fold(
                    onSuccess = { entries ->
                        val recent = entries
                            .sortedByDescending { it.date }
                            .take(5)
                            .map { entryMapper.mapToUiModel(it) }
                        _uiState.update { it.copy(recentEntries = recent) }
                    },
                    onFailure = { /* secondary data — silently ignored */ }
                )
            }
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            getUserProfileUseCase().collect { profile ->
                val initial = profile.name.take(1).uppercase()
                val formattedTarget = if (profile.targetAmount > 0.0) {
                    "€${String.format(java.util.Locale.US, "%,.0f", profile.targetAmount)}"
                } else ""
                _uiState.update { it.copy(userName = profile.name, userInitial = initial, targetAmount = formattedTarget) }
            }
        }
    }

    private fun saveEntry() {
        val input = _uiState.value.entryInput.toDoubleOrNull() ?: return
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSaving = true) }
            val entry = NetWorthEntry(value = input, date = _uiState.value.selectedDate)
            addNetWorthEntryUseCase(entry).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, isAddEntrySheetVisible = false, entryInput = "", selectedDate = LocalDate.now()) }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(isSaving = false, error = error.message) }
                }
            )
        }
    }
}
