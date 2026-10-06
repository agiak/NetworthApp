package com.agcoding.networkapp.account.domain.usecase

import com.agcoding.networkapp.account.domain.repository.AccountRepository
import javax.inject.Inject

class UpdateAccountSalaryUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(accountId: Long, salary: Double) =
        repository.updateMonthlySalary(accountId, salary.coerceAtLeast(0.0))
}
