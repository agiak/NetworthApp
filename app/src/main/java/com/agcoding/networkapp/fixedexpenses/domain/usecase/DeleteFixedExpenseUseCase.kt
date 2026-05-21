package com.agcoding.networkapp.fixedexpenses.domain.usecase

interface DeleteFixedExpenseUseCase {
    suspend operator fun invoke(id: Long): Result<Unit>
}
