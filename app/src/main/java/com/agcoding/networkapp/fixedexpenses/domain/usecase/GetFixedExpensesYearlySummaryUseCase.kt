package com.agcoding.networkapp.fixedexpenses.domain.usecase

import kotlinx.coroutines.flow.Flow

interface GetFixedExpensesYearlySummaryUseCase {
    operator fun invoke(): Flow<Double>
}
