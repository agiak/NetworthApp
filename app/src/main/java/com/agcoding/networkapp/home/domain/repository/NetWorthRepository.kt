package com.agcoding.networkapp.home.domain.repository

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import kotlinx.coroutines.flow.Flow

interface NetWorthRepository {
    fun getEntries(): Flow<Result<List<NetWorthEntry>>>
    fun getEntriesForAccount(accountId: Long): Flow<Result<List<NetWorthEntry>>>
    fun getEntryById(id: Long): Flow<Result<NetWorthEntry?>>
    suspend fun addEntry(entry: NetWorthEntry): Result<Unit>
    suspend fun updateEntry(entry: NetWorthEntry): Result<Unit>
    suspend fun deleteEntry(id: Long): Result<Unit>
    suspend fun deleteEntriesForAccount(accountId: Long): Result<Unit>
    suspend fun deleteAllEntries(): Result<Unit>
}
