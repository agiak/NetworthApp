package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import javax.inject.Inject

class UpdateNetWorthEntryUseCaseImpl @Inject constructor(
    private val repository: NetWorthRepository
) : UpdateNetWorthEntryUseCase {
    override suspend fun invoke(entry: NetWorthEntry): Result<Unit> = repository.updateEntry(entry)
}
