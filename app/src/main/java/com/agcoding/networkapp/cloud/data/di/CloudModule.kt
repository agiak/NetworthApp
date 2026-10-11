package com.agcoding.networkapp.cloud.data.di

import com.agcoding.networkapp.cloud.data.local.SyncLinkDao
import com.agcoding.networkapp.home.data.local.NetWorthDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CloudModule {

    @Provides
    fun provideSyncLinkDao(db: NetWorthDatabase): SyncLinkDao = db.syncLinkDao()
}
