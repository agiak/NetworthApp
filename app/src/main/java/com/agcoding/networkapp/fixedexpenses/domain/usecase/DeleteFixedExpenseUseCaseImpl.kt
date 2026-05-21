package com.agcoding.networkapp.fixedexpenses.domain.usecase

import com.agcoding.networkapp.fixedexpenses.domain.repository.FixedExpensesRepository
import javax.inject.Inject

class DeleteFixedExpenseUseCaseImpl @Inject constructor(
    private val repository: FixedExpensesRepository,
) : DeleteFixedExpenseUseCase {
    override suspend operator fun invoke(id: Long): Result<Unit> = repository.delete(id)
}
