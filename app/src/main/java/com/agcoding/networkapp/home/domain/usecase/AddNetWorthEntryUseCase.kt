package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry

interface AddNetWorthEntryUseCase {
    suspend operator fun invoke(entry: NetWorthEntry): Result<Unit>
}
