package com.agcoding.networkapp.fixedexpenses.domain.usecase

import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import kotlinx.coroutines.flow.Flow

interface GetFixedExpensesUseCase {
    operator fun invoke(): Flow<Result<List<FixedExpense>>>
}
