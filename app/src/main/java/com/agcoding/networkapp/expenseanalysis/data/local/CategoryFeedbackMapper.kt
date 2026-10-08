package com.agcoding.networkapp.expenseanalysis.data.local

import com.agcoding.networkapp.expenseanalysis.domain.model.CategoryFeedback
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory

/** Null when the stored category no longer exists. */
fun CategoryFeedbackEntity.toDomain(): CategoryFeedback? =
    ExpenseCategory.fromName(category)?.let { CategoryFeedback(nameKey, it) }

fun CategoryFeedback.toEntity(): CategoryFeedbackEntity =
    CategoryFeedbackEntity(nameKey = nameKey, category = category.name)
