package com.agcoding.networkapp.expenseanalysis.domain.usecase

import com.agcoding.networkapp.expenseanalysis.domain.categorizer.TextNormalizer
import com.agcoding.networkapp.expenseanalysis.domain.model.CategoryFeedback
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory
import com.agcoding.networkapp.expenseanalysis.domain.repository.CategoryFeedbackRepository
import javax.inject.Inject

/**
 * Remembers the category the user picked for an expense name (and teaches the categoriser
 * its words). A null [category] forgets the choice and goes back to automatic.
 */
class SetExpenseCategoryUseCase @Inject constructor(
    private val repository: CategoryFeedbackRepository,
) {
    suspend operator fun invoke(expenseTitle: String, category: ExpenseCategory?): Result<Unit> = runCatching {
        val key = TextNormalizer.key(expenseTitle)
        require(key.isNotEmpty()) { "Expense name has no words to learn from" }
        if (category == null) repository.remove(key)
        else repository.set(CategoryFeedback(key, category))
    }
}
