package com.agcoding.networkapp.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.usecase.CreateAccountUseCase
import com.agcoding.networkapp.shared.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateAccountUiState(
    val name: String = "",
    val startingBalance: String = "",
    val colorHex: String = Account.PRESET_COLORS.first(),
    val isCreating: Boolean = false,
    val isCreated: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null,
) {
    val canCreate get() = name.isNotBlank()
}

sealed interface CreateAccountIntent {
    data class UpdateName(val value: String) : CreateAccountIntent
    data class UpdateStartingBalance(val value: String) : CreateAccountIntent
    data class SelectColor(val colorHex: String) : CreateAccountIntent
    data object Create : CreateAccountIntent
    data object ClearError : CreateAccountIntent
}

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val createAccountUseCase: CreateAccountUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAccountUiState())
    val uiState: StateFlow<CreateAccountUiState> = _uiState.asStateFlow()

    fun onIntent(intent: CreateAccountIntent) {
        when (intent) {
            is CreateAccountIntent.UpdateName           -> _uiState.update { it.copy(name = intent.value) }
            is CreateAccountIntent.UpdateStartingBalance -> _uiState.update { it.copy(startingBalance = intent.value) }
            is CreateAccountIntent.SelectColor          -> _uiState.update { it.copy(colorHex = intent.colorHex) }
            CreateAccountIntent.Create                  -> createAccount()
            CreateAccountIntent.ClearError              -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun createAccount() {
        val state = _uiState.value
        if (!state.canCreate) return
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isCreating = true) }
            try {
                createAccountUseCase(
                    Account(
                        name            = state.name.trim(),
                        startingBalance = state.startingBalance.toDoubleOrNull() ?: 0.0,
                        colorHex        = state.colorHex,
                    )
                )
                _uiState.update { it.copy(isCreating = false, isCreated = true) }
                delay(1800)
                _uiState.update { it.copy(isComplete = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreating = false, error = e.message) }
            }
        }
    }
}
