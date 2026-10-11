package com.agcoding.networkapp.cloud.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SyncLinkDao {

    @Query("SELECT * FROM sync_links WHERE entity = :entity")
    suspend fun getAll(entity: String): List<SyncLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(links: List<SyncLinkEntity>)

    @Query("DELETE FROM sync_links WHERE entity = :entity")
    suspend fun deleteAll(entity: String)

    @Transaction
    suspend fun replaceAll(entity: String, links: List<SyncLinkEntity>) {
        deleteAll(entity)
        upsertAll(links)
    }

    @Query("DELETE FROM sync_links")
    suspend fun clear()
}
