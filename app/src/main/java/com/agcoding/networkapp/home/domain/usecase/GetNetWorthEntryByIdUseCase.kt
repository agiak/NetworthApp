package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import kotlinx.coroutines.flow.Flow

interface GetNetWorthEntryByIdUseCase {
    operator fun invoke(id: Long): Flow<Result<NetWorthEntry?>>
}
