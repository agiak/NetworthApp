package com.agcoding.networkapp.history.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntryByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val getNetWorthEntryByIdUseCase: GetNetWorthEntryByIdUseCase
) : ViewModel() {

    private val entryId: Long = checkNotNull(savedStateHandle["entryId"])

    private val _uiState = MutableStateFlow(EntryDetailsUiState(entryId = entryId))
    val uiState: StateFlow<EntryDetailsUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            getNetWorthEntryByIdUseCase(entryId).collect { result ->
                result.fold(
                    onSuccess = { entry ->
                        if (entry != null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    formattedAmount = "€${String.format(Locale.US, "%,.0f", entry.value)}",
                                    formattedDate = entry.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
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
