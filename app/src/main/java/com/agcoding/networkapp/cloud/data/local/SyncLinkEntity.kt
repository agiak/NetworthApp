package com.agcoding.networkapp.cloud.data.local

import androidx.room.Entity

/**
 * Which server row a local row is synced with, and the hash of both as of the last sync.
 * [entity] is a RemoteTable name; [localKey] is the local id (or the name key for categories).
 */
@Entity(tableName = "sync_links", primaryKeys = ["entity", "localKey"])
data class SyncLinkEntity(
    val entity: String,
    val localKey: String,
    val remoteId: String,
    val hash: String,
)
