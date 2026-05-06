package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import javax.inject.Inject

class AddNetWorthEntryUseCaseImpl @Inject constructor(
    private val repository: NetWorthRepository
) : AddNetWorthEntryUseCase {
    override suspend operator fun invoke(entry: NetWorthEntry): Result<Unit> = repository.addEntry(entry)
}
