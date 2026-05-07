package com.agcoding.networkapp.home.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NetWorthDao {
    @Query("SELECT * FROM net_worth_entries ORDER BY dateEpochDay DESC")
    fun getAllEntries(): Flow<List<NetWorthEntity>>

    @Query("SELECT * FROM net_worth_entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<NetWorthEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: NetWorthEntity)

    @Update
    suspend fun updateEntry(entry: NetWorthEntity)

    @Query("DELETE FROM net_worth_entries")
    suspend fun deleteAllEntries()
}
