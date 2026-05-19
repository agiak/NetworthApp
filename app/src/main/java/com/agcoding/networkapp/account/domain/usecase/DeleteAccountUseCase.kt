package com.agcoding.networkapp.account.domain.usecase

import com.agcoding.networkapp.account.domain.repository.AccountRepository
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val netWorthRepository: NetWorthRepository,
) {
    suspend operator fun invoke(id: Long) {
        netWorthRepository.deleteEntriesForAccount(id)
        accountRepository.deleteAccount(id)
    }
}
