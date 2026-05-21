package com.agcoding.networkapp.fixedexpenses.domain.usecase

import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.domain.repository.FixedExpensesRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetFixedExpensesYearlySummaryUseCaseImpl @Inject constructor(
    private val repository: FixedExpensesRepository,
) : GetFixedExpensesYearlySummaryUseCase {

    override operator fun invoke() = repository.getAll()
        .map { result ->
            result.getOrDefault(emptyList()).sumOf { expense ->
                when (expense.recurrence) {
                    RecurrenceType.MONTHLY -> expense.cost * 12.0
                    RecurrenceType.ANNUAL  -> expense.cost
                }
            }
        }
        .catch { emit(0.0) }
}
