package com.agcoding.networkapp.home.domain.repository

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import kotlinx.coroutines.flow.Flow

interface NetWorthRepository {
    fun getEntries(): Flow<Result<List<NetWorthEntry>>>
    suspend fun addEntry(entry: NetWorthEntry): Result<Unit>
    suspend fun deleteAllEntries(): Result<Unit>
}
