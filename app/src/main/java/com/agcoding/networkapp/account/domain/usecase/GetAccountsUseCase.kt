package com.agcoding.networkapp.account.domain.usecase

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    operator fun invoke(): Flow<List<Account>> = repository.getAccounts()
}
