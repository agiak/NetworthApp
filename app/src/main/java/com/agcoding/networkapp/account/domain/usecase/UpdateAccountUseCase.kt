package com.agcoding.networkapp.account.domain.usecase

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.repository.AccountRepository
import javax.inject.Inject

class UpdateAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(account: Account) = repository.updateAccount(account)
}
