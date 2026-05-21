package com.agcoding.networkapp.fixedexpenses.domain.usecase

import com.agcoding.networkapp.fixedexpenses.domain.repository.FixedExpensesRepository
import javax.inject.Inject

class GetFixedExpensesUseCaseImpl @Inject constructor(
    private val repository: FixedExpensesRepository,
) : GetFixedExpensesUseCase {
    override operator fun invoke() = repository.getAll()
}
