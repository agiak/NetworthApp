package com.agcoding.networkapp.home.data.di

import android.content.Context
import androidx.room.Room
import com.agcoding.networkapp.home.data.local.NetWorthDao
import com.agcoding.networkapp.home.data.local.NetWorthDatabase
import com.agcoding.networkapp.home.data.repository.NetWorthRepositoryImpl
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetWorthDataModule {

    @Binds
    @Singleton
    abstract fun bindNetWorthRepository(impl: NetWorthRepositoryImpl): NetWorthRepository

    companion object {
        @Provides
        @Singleton
        fun provideNetWorthDatabase(@ApplicationContext context: Context): NetWorthDatabase =
            Room.databaseBuilder(context, NetWorthDatabase::class.java, "net_worth_db")
                .fallbackToDestructiveMigration()
                .build()

        @Provides
        fun provideNetWorthDao(db: NetWorthDatabase): NetWorthDao = db.netWorthDao()
    }
}
