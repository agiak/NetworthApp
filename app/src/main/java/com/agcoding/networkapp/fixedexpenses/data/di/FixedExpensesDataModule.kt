package com.agcoding.networkapp.fixedexpenses.data.di

import com.agcoding.networkapp.fixedexpenses.data.local.FixedExpenseDao
import com.agcoding.networkapp.fixedexpenses.data.repository.FixedExpensesRepositoryImpl
import com.agcoding.networkapp.fixedexpenses.domain.repository.FixedExpensesRepository
import com.agcoding.networkapp.home.data.local.NetWorthDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FixedExpensesDataModule {

    @Binds
    @Singleton
    abstract fun bindFixedExpensesRepository(impl: FixedExpensesRepositoryImpl): FixedExpensesRepository

    companion object {

        @Provides
        fun provideFixedExpenseDao(db: NetWorthDatabase): FixedExpenseDao = db.fixedExpenseDao()
    }
}
