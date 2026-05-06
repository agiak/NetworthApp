package com.agcoding.networkapp.home.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [NetWorthEntity::class], version = 2, exportSchema = false)
abstract class NetWorthDatabase : RoomDatabase() {
    abstract fun netWorthDao(): NetWorthDao
}
