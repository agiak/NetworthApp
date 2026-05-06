package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import kotlinx.coroutines.flow.Flow

interface GetNetWorthEntriesUseCase {
    operator fun invoke(): Flow<Result<List<NetWorthEntry>>>
}
