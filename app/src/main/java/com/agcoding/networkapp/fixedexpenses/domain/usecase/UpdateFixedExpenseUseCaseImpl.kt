package com.agcoding.networkapp.fixedexpenses.domain.usecase

import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.repository.FixedExpensesRepository
import javax.inject.Inject

class UpdateFixedExpenseUseCaseImpl @Inject constructor(
    private val repository: FixedExpensesRepository,
) : UpdateFixedExpenseUseCase {
    override suspend operator fun invoke(expense: FixedExpense): Result<Unit> = repository.update(expense)
}
