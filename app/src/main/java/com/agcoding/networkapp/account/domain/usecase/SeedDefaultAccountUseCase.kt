package com.agcoding.networkapp.account.domain.usecase

import com.agcoding.networkapp.account.domain.repository.AccountRepository
import javax.inject.Inject

class SeedDefaultAccountUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    suspend operator fun invoke() = repository.seedDefaultAccountIfNeeded()
}
