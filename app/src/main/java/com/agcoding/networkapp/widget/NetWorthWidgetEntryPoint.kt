package com.agcoding.networkapp.widget

import com.agcoding.networkapp.home.data.local.NetWorthDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetWorthWidgetEntryPoint {
    fun netWorthDao(): NetWorthDao
}
