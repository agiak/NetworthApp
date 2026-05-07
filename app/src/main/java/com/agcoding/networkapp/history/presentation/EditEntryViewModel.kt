package com.agcoding.networkapp.history.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.usecase.GetNetWorthEntryByIdUseCase
import com.agcoding.networkapp.home.domain.usecase.UpdateNetWorthEntryUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class EditEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNetWorthEntryByIdUseCase: GetNetWorthEntryByIdUseCase,
    private val updateNetWorthEntryUseCase: UpdateNetWorthEntryUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val entryId: Long = checkNotNull(savedStateHandle["entryId"])

    private val _uiState = MutableStateFlow(EditEntryUiState())
    val uiState: StateFlow<EditEntryUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
    }

    fun onIntent(intent: EditEntryIntent) {
        when (intent) {
            is EditEntryIntent.UpdateAmount -> _uiState.update { it.copy(amountInput = intent.value) }
            is EditEntryIntent.UpdateDate -> _uiState.update { it.copy(selectedDate = intent.date) }
            EditEntryIntent.Save -> saveEntry()
            EditEntryIntent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            getNetWorthEntryByIdUseCase(entryId).first().fold(
                onSuccess = { entry ->
                    if (entry != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                amountInput = entry.value.toLong().toString(),
                                selectedDate = entry.date
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

    private fun saveEntry() {
        val amount = _uiState.value.amountInput.toDoubleOrNull() ?: return
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSaving = true) }
            val entry = NetWorthEntry(id = entryId, value = amount, date = _uiState.value.selectedDate)
            updateNetWorthEntryUseCase(entry).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, isComplete = true) }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(isSaving = false, error = error.message) }
                }
            )
        }
    }
}
