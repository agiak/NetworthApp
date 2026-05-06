package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import kotlinx.coroutines.flow.Flow

interface GetMonthlyNetWorthUseCase {
    operator fun invoke(): Flow<Result<List<MonthlyNetWorth>>>
}
