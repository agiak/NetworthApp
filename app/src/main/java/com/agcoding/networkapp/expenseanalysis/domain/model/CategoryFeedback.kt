package com.agcoding.networkapp.expenseanalysis.domain.model

/**
 * A category the user chose for an expense name. Kept even if the expense is deleted or
 * renamed, so the categoriser keeps learning from it.
 *
 * @param nameKey normalised expense name (see TextNormalizer.key)
 */
data class CategoryFeedback(
    val nameKey: String,
    val category: ExpenseCategory,
)
