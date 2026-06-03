package com.agcoding.networkapp.account.domain.usecase

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.repository.AccountRepository
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import java.time.LocalDate
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val netWorthRepository: NetWorthRepository,
) {
    suspend operator fun invoke(account: Account): Long {
        val id = accountRepository.createAccount(account)
        if (account.startingBalance > 0.0) {
            netWorthRepository.addEntry(
                NetWorthEntry(
                    value     = account.startingBalance,
                    date      = LocalDate.now(),
                    accountId = id,
                )
            )
        }
        return id
    }
}
