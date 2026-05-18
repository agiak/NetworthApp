package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import javax.inject.Inject

class DeleteNetWorthEntryUseCaseImpl @Inject constructor(
    private val repository: NetWorthRepository
) : DeleteNetWorthEntryUseCase {
    override suspend operator fun invoke(id: Long): Result<Unit> = repository.deleteEntry(id)
}
