package com.agcoding.networkapp.expenseanalysis.data.repository

import com.agcoding.networkapp.backup.data.AutoBackupDataSource
import com.agcoding.networkapp.expenseanalysis.data.local.CategoryFeedbackDao
import com.agcoding.networkapp.expenseanalysis.data.local.toDomain
import com.agcoding.networkapp.expenseanalysis.data.local.toEntity
import com.agcoding.networkapp.expenseanalysis.domain.model.CategoryFeedback
import com.agcoding.networkapp.expenseanalysis.domain.repository.CategoryFeedbackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryFeedbackRepositoryImpl @Inject constructor(
    private val dao: CategoryFeedbackDao,
    private val autoBackup: AutoBackupDataSource,
) : CategoryFeedbackRepository {

    override fun getAll(): Flow<List<CategoryFeedback>> =
        dao.getAll().map { entities -> entities.mapNotNull { it.toDomain() } }

    override suspend fun set(feedback: CategoryFeedback) {
        dao.upsert(feedback.toEntity())
        autoBackup.trigger()
    }

    override suspend fun remove(nameKey: String) {
        dao.delete(nameKey)
        autoBackup.trigger()
    }
}
