package com.agcoding.networkapp.cloud.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncLinkDao {

    @Query("SELECT * FROM sync_links WHERE entity = :entity")
    suspend fun getAll(entity: String): List<SyncLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(links: List<SyncLinkEntity>)

    @Query("DELETE FROM sync_links WHERE entity = :entity")
    suspend fun deleteAll(entity: String)

    @Query("DELETE FROM sync_links")
    suspend fun clear()
}
