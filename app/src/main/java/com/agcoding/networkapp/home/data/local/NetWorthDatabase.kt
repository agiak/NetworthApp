package com.agcoding.networkapp.home.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agcoding.networkapp.account.data.local.AccountDao
import com.agcoding.networkapp.account.data.local.AccountEntity

@Database(
    entities = [NetWorthEntity::class, AccountEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class NetWorthDatabase : RoomDatabase() {
    abstract fun netWorthDao(): NetWorthDao
    abstract fun accountDao(): AccountDao
}
