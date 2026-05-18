package com.agcoding.networkapp.history.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.home.domain.usecase.DeleteNetWorthEntryUseCase
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntryByIdUseCase
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.usecase.GetAppCurrencyUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EntryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNetWorthEntryByIdUseCase: GetNetWorthEntryByIdUseCase,
    private val deleteNetWorthEntryUseCase: DeleteNetWorthEntryUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val entryId: Long = checkNotNull(savedStateHandle["entryId"])

    private val _uiState = MutableStateFlow(EntryDetailsUiState(entryId = entryId))
    val uiState: StateFlow<EntryDetailsUiState> = _uiState.asStateFlow()

    private var currentCurrency: AppCurrency = AppCurrency.EUR
    private var cachedValue: Double? = null

    init {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currency ->
                currentCurrency = currency
                cachedValue?.let { value ->
                    _uiState.update { it.copy(formattedAmount = "${currency.symbol}${String.format(Locale.US, "%,.0f", value)}") }
                }
            }
        }
        loadEntry()
    }

    fun deleteEntry() {
        viewModelScope.launch(ioDispatcher) {
            deleteNetWorthEntryUseCase(entryId).fold(
                onSuccess = { _uiState.update { it.copy(isDeleted = true) } },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            getNetWorthEntryByIdUseCase(entryId).collect { result ->
                result.fold(
                    onSuccess = { entry ->
                        if (entry != null) {
                            cachedValue = entry.value
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    formattedAmount = "${currentCurrency.symbol}${String.format(Locale.US, "%,.0f", entry.value)}",
                                    formattedDate = entry.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())),
                                    note = entry.note,
                                )
                            }
                        } else {
                            _uiState.update { it.copy(isLoading = false, error = "Entry not found") }
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
}
