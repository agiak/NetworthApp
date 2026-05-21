package com.agcoding.networkapp.fixedexpenses.data.repository

import com.agcoding.networkapp.fixedexpenses.data.local.FixedExpenseDao
import com.agcoding.networkapp.fixedexpenses.data.mapper.FixedExpenseEntityToDomainMapper
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.repository.FixedExpensesRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixedExpensesRepositoryImpl @Inject constructor(
    private val dao: FixedExpenseDao,
    private val mapper: FixedExpenseEntityToDomainMapper,
) : FixedExpensesRepository {

    override fun getAll() = dao.getAll()
        .map { entities -> Result.success(entities.map(mapper::toDomain)) }
        .catch { emit(Result.failure(it)) }

    override suspend fun add(expense: FixedExpense): Result<Unit> = runCatching {
        dao.insert(mapper.toEntity(expense))
    }

    override suspend fun update(expense: FixedExpense): Result<Unit> = runCatching {
        dao.update(mapper.toEntity(expense))
    }

    override suspend fun delete(id: Long): Result<Unit> = runCatching {
        dao.deleteById(id)
    }
}
