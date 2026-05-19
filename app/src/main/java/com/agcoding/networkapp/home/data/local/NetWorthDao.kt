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

    @Query("SELECT * FROM net_worth_entries WHERE accountId = :accountId ORDER BY dateEpochDay DESC")
    fun getEntriesForAccount(accountId: Long): Flow<List<NetWorthEntity>>

    @Query("SELECT * FROM net_worth_entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<NetWorthEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: NetWorthEntity)

    @Update
    suspend fun updateEntry(entry: NetWorthEntity)

    @Query("SELECT * FROM net_worth_entries ORDER BY dateEpochDay DESC LIMIT 2")
    suspend fun getLatestTwoEntries(): List<NetWorthEntity>

    @Query("DELETE FROM net_worth_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("SELECT * FROM net_worth_entries ORDER BY dateEpochDay DESC")
    suspend fun getAllEntriesOnce(): List<NetWorthEntity>

    @Query("DELETE FROM net_worth_entries WHERE accountId = :accountId")
    suspend fun deleteEntriesForAccount(accountId: Long)

    @Query("DELETE FROM net_worth_entries")
    suspend fun deleteAllEntries()
}
