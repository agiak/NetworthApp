package com.agcoding.networkapp.snapshot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.usecase.GetAccountsUseCase
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.usecase.AddNetWorthEntryUseCase
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
import java.time.LocalDate
import javax.inject.Inject

data class AddSnapshotUiState(
    val entryInput: String = "",
    val noteInput: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val currencySymbol: String = "€",
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long = 1L,
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
    val error: String? = null,
)

sealed interface AddSnapshotIntent {
    data class UpdateInput(val value: String) : AddSnapshotIntent
    data class UpdateDate(val date: LocalDate) : AddSnapshotIntent
    data class UpdateNote(val value: String) : AddSnapshotIntent
    data class SelectAccount(val accountId: Long) : AddSnapshotIntent
    data object Save : AddSnapshotIntent
    data object ClearError : AddSnapshotIntent
}

@HiltViewModel
class AddSnapshotViewModel @Inject constructor(
    private val addNetWorthEntryUseCase: AddNetWorthEntryUseCase,
    private val getAppCurrencyUseCase: GetAppCurrencyUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddSnapshotUiState())
    val uiState: StateFlow<AddSnapshotUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getAppCurrencyUseCase().collect { currency: AppCurrency ->
                _uiState.update { it.copy(currencySymbol = currency.symbol) }
            }
        }
        viewModelScope.launch {
            getAccountsUseCase().collect { accounts ->
                val currentId = _uiState.value.selectedAccountId
                val validId = if (accounts.any { it.id == currentId }) currentId
                              else accounts.firstOrNull()?.id ?: 1L
                _uiState.update { it.copy(accounts = accounts, selectedAccountId = validId) }
            }
        }
    }

    fun onIntent(intent: AddSnapshotIntent) {
        when (intent) {
            is AddSnapshotIntent.UpdateInput   -> _uiState.update { it.copy(entryInput = intent.value) }
            is AddSnapshotIntent.UpdateDate    -> _uiState.update { it.copy(selectedDate = intent.date) }
            is AddSnapshotIntent.UpdateNote    -> _uiState.update { it.copy(noteInput = intent.value) }
            is AddSnapshotIntent.SelectAccount -> _uiState.update { it.copy(selectedAccountId = intent.accountId) }
            AddSnapshotIntent.Save             -> save()
            AddSnapshotIntent.ClearError       -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun save() {
        val input = _uiState.value.entryInput.toDoubleOrNull() ?: return
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSaving = true) }
            val entry = NetWorthEntry(
                value     = input,
                date      = _uiState.value.selectedDate,
                note      = _uiState.value.noteInput,
                accountId = _uiState.value.selectedAccountId,
            )
            addNetWorthEntryUseCase(entry).fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false, isDone = true) } },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(isSaving = false, error = error.message) }
                }
            )
        }
    }
}
