package com.agcoding.networkapp.expenseanalysis.data.di

import com.agcoding.networkapp.expenseanalysis.data.local.CategoryFeedbackDao
import com.agcoding.networkapp.expenseanalysis.data.repository.CategoryFeedbackRepositoryImpl
import com.agcoding.networkapp.expenseanalysis.domain.repository.CategoryFeedbackRepository
import com.agcoding.networkapp.home.data.local.NetWorthDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExpenseAnalysisDataModule {

    @Binds
    @Singleton
    abstract fun bindCategoryFeedbackRepository(impl: CategoryFeedbackRepositoryImpl): CategoryFeedbackRepository

    companion object {

        @Provides
        fun provideCategoryFeedbackDao(db: NetWorthDatabase): CategoryFeedbackDao = db.categoryFeedbackDao()
    }
}
