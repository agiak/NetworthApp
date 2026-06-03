package com.agcoding.networkapp.account.domain.usecase

import com.agcoding.networkapp.account.domain.repository.AccountRepository
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import java.time.LocalDate
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val netWorthRepository: NetWorthRepository,
) {
    suspend operator fun invoke(id: Long) {
        // Insert a tombstone entry (value=0) so aggregateToMonthly carries 0 forward from this
        // month onward. Historical entries are preserved for accurate statistics up to deletion.
        netWorthRepository.addEntry(
            NetWorthEntry(
                value     = 0.0,
                date      = LocalDate.now(),
                note      = NetWorthEntry.DELETION_MARKER,
                accountId = id,
            )
        )
        accountRepository.deleteAccount(id)
    }
}
