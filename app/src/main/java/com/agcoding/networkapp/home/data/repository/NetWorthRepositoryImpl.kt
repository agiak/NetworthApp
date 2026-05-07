package com.agcoding.networkapp.home.data.repository

import com.agcoding.networkapp.home.data.local.NetWorthDao
import com.agcoding.networkapp.home.data.local.NetWorthEntity
import com.agcoding.networkapp.home.data.mapper.NetWorthEntityToDomainMapper
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import com.agcoding.networkapp.shared.di.IoDispatcher
import com.agcoding.networkapp.shared.domain.error.AppError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NetWorthRepositoryImpl @Inject constructor(
    private val dao: NetWorthDao,
    private val mapper: NetWorthEntityToDomainMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : NetWorthRepository {

    override fun getEntries(): Flow<Result<List<NetWorthEntry>>> = dao.getAllEntries()
        .map { entities -> Result.success(entities.map { mapper.map(it) }) }
        .catch { e -> emit(Result.failure(AppError.DatabaseError(e.message ?: "Database error"))) }
        .flowOn(ioDispatcher)

    override fun getEntryById(id: Long): Flow<Result<NetWorthEntry?>> = dao.getEntryById(id)
        .map { entity -> Result.success(entity?.let { mapper.map(it) }) }
        .catch { e -> emit(Result.failure(AppError.DatabaseError(e.message ?: "Database error"))) }
        .flowOn(ioDispatcher)

    override suspend fun addEntry(entry: NetWorthEntry): Result<Unit> = withContext(ioDispatcher) {
        try {
            dao.insertEntry(NetWorthEntity(value = entry.value, dateEpochDay = entry.date.toEpochDay()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.message ?: "Failed to save entry"))
        }
    }

    override suspend fun updateEntry(entry: NetWorthEntry): Result<Unit> = withContext(ioDispatcher) {
        try {
            dao.updateEntry(NetWorthEntity(id = entry.id, value = entry.value, dateEpochDay = entry.date.toEpochDay()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.message ?: "Failed to update entry"))
        }
    }

    override suspend fun deleteAllEntries(): Result<Unit> = withContext(ioDispatcher) {
        try {
            dao.deleteAllEntries()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.message ?: "Failed to delete entries"))
        }
    }
}
