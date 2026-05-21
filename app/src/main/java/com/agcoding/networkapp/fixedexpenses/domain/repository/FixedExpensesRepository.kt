package com.agcoding.networkapp.fixedexpenses.domain.repository

import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import kotlinx.coroutines.flow.Flow

interface FixedExpensesRepository {
    fun getAll(): Flow<Result<List<FixedExpense>>>
    suspend fun add(expense: FixedExpense): Result<Unit>
    suspend fun update(expense: FixedExpense): Result<Unit>
    suspend fun delete(id: Long): Result<Unit>
    suspend fun deleteAll(): Result<Unit>
}
