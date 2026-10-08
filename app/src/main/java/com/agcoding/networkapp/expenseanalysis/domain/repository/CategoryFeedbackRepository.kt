package com.agcoding.networkapp.expenseanalysis.domain.repository

import com.agcoding.networkapp.expenseanalysis.domain.model.CategoryFeedback
import kotlinx.coroutines.flow.Flow

interface CategoryFeedbackRepository {
    fun getAll(): Flow<List<CategoryFeedback>>
    suspend fun set(feedback: CategoryFeedback)
    suspend fun remove(nameKey: String)
}
