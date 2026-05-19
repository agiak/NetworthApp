package com.agcoding.networkapp.history.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.account.domain.usecase.GetAccountsUseCase
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
import javax.inject.Inject

@HiltViewModel
class EditEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNetWorthEntryByIdUseCase: GetNetWorthEntryByIdUseCase,
    private val updateNetWorthEntryUseCase: UpdateNetWorthEntryUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val entryId: Long = checkNotNull(savedStateHandle["entryId"])

    private val _uiState = MutableStateFlow(EditEntryUiState())
    val uiState: StateFlow<EditEntryUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
        viewModelScope.launch {
            getAccountsUseCase().collect { accounts ->
                val current = _uiState.value.selectedAccountId
                val validId = if (accounts.any { it.id == current }) current
                              else accounts.firstOrNull()?.id ?: 1L
                _uiState.update { it.copy(accounts = accounts, selectedAccountId = validId) }
            }
        }
    }

    fun onIntent(intent: EditEntryIntent) {
        when (intent) {
            is EditEntryIntent.UpdateAmount   -> _uiState.update { it.copy(amountInput = intent.value) }
            is EditEntryIntent.UpdateDate     -> _uiState.update { it.copy(selectedDate = intent.date) }
            is EditEntryIntent.UpdateNote     -> _uiState.update { it.copy(noteInput = intent.value) }
            is EditEntryIntent.SelectAccount  -> _uiState.update { it.copy(selectedAccountId = intent.accountId) }
            EditEntryIntent.Save              -> saveEntry()
            EditEntryIntent.ClearError        -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            getNetWorthEntryByIdUseCase(entryId).first().fold(
                onSuccess = { entry ->
                    if (entry != null) {
                        _uiState.update {
                            it.copy(
                                isLoading         = false,
                                amountInput       = entry.value.toLong().toString(),
                                selectedDate      = entry.date,
                                noteInput         = entry.note,
                                selectedAccountId = entry.accountId,
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
            val entry = NetWorthEntry(
                id        = entryId,
                value     = amount,
                date      = _uiState.value.selectedDate,
                note      = _uiState.value.noteInput,
                accountId = _uiState.value.selectedAccountId,
            )
            updateNetWorthEntryUseCase(entry).fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false, isComplete = true) } },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(isSaving = false, error = error.message) }
                }
            )
        }
    }
}
