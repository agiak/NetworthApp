package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNetWorthEntryByIdUseCaseImpl @Inject constructor(
    private val repository: NetWorthRepository
) : GetNetWorthEntryByIdUseCase {
    override fun invoke(id: Long): Flow<Result<NetWorthEntry?>> = repository.getEntryById(id)
}
