package com.agcoding.networkapp.home.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NetWorthDao {
    @Query("SELECT * FROM net_worth_entries ORDER BY dateEpochDay DESC")
    fun getAllEntries(): Flow<List<NetWorthEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: NetWorthEntity)

    @Query("DELETE FROM net_worth_entries")
    suspend fun deleteAllEntries()
}
