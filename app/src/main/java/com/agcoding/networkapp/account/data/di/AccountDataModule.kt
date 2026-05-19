package com.agcoding.networkapp.account.data.di

import com.agcoding.networkapp.account.data.local.AccountDao
import com.agcoding.networkapp.account.data.repository.AccountRepositoryImpl
import com.agcoding.networkapp.account.domain.repository.AccountRepository
import com.agcoding.networkapp.home.data.local.NetWorthDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountDataModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    companion object {
        @Provides
        fun provideAccountDao(db: NetWorthDatabase): AccountDao = db.accountDao()
    }
}
