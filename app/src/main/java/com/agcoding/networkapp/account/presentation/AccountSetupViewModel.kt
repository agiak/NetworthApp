package com.agcoding.networkapp.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.usecase.GetAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AccountSetupViewModel @Inject constructor(
    getAccountsUseCase: GetAccountsUseCase,
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = getAccountsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
