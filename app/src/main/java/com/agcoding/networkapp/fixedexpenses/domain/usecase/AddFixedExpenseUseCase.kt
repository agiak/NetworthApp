package com.agcoding.networkapp.fixedexpenses.domain.usecase

import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense

interface AddFixedExpenseUseCase {
    suspend operator fun invoke(expense: FixedExpense): Result<Unit>
}
