package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNetWorthEntriesUseCaseImpl @Inject constructor(
    private val repository: NetWorthRepository
) : GetNetWorthEntriesUseCase {
    override operator fun invoke(): Flow<Result<List<NetWorthEntry>>> = repository.getEntries()
}
