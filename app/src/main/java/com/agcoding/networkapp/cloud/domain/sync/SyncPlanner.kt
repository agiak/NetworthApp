package com.agcoding.networkapp.cloud.domain.sync

import java.security.MessageDigest

/** A row as this device has it, with fields already in their synced form (see [SyncFields]). */
data class LocalItem(
    val localKey: String,
    val fields: Map<String, String?>,
)

/** A row as the server has it. */
data class RemoteItem(
    val remoteId: String,
    val fields: Map<String, String?>,
    val deleted: Boolean = false,
)

/**
 * What both sides looked like the last time a row was synced. Comparing each side's current
 * [hashOf] with [hash] tells which side changed since; an empty hash means "never confirmed"
 * (pushed but not yet acknowledged).
 */
data class SyncLink(
    val localKey: String,
    val remoteId: String,
    val hash: String,
)

data class EntityPlan(
    /** Rows to create or update on the server: remote id → fields. */
    val push: Map<String, Map<String, String?>>,
    /** Server rows to mark as deleted. */
    val softDelete: List<String>,
    /** Server rows this device doesn't have yet. */
    val insertLocal: List<RemoteItem>,
    /** Local rows to overwrite with the server's version: local key → remote row. */
    val updateLocal: Map<String, RemoteItem>,
    /** Local rows deleted on another device. */
    val deleteLocal: List<String>,
    /** Links after the sync, for every row except [insertLocal] (their local key is known after inserting). */
    val links: List<SyncLink>,
)

fun hashOf(fields: Map<String, String?>): String {
    val canonical = fields.toSortedMap().entries.joinToString("\u001F") { (key, value) ->
        "$key=${value ?: "\u0000"}"
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

/**
 * Three-way merge of one table.
 *
 * - Changed on one side only → that side wins.
 * - Changed on both sides → this device wins (its version is pushed).
 * - Deleted on one side → deleted on the other, unless the other side changed it since.
 * - New on either side → copied to the other.
 *
 * @param newRemoteId id for a local row that has never been synced
 */
fun planSync(
    local: List<LocalItem>,
    remote: List<RemoteItem>,
    links: List<SyncLink>,
    newRemoteId: (LocalItem) -> String,
): EntityPlan {
    val localByKey = local.associateBy { it.localKey }
    val remoteById = remote.associateBy { it.remoteId }

    val push = linkedMapOf<String, Map<String, String?>>()
    val softDelete = mutableListOf<String>()
    val insertLocal = mutableListOf<RemoteItem>()
    val updateLocal = linkedMapOf<String, RemoteItem>()
    val deleteLocal = mutableListOf<String>()
    val newLinks = mutableListOf<SyncLink>()

    val linkedLocal = mutableSetOf<String>()
    val linkedRemote = mutableSetOf<String>()

    fun pushLocal(item: LocalItem, remoteId: String) {
        push[remoteId] = item.fields
        newLinks += SyncLink(item.localKey, remoteId, hashOf(item.fields))
    }

    links.forEach { link ->
        // A row can only be linked once; ignore stale duplicates
        if (link.localKey in linkedLocal || link.remoteId in linkedRemote) return@forEach
        val l = localByKey[link.localKey]
        val r = remoteById[link.remoteId]
        linkedLocal += link.localKey
        linkedRemote += link.remoteId

        val localChanged = l != null && hashOf(l.fields) != link.hash
        when {
            l == null && r == null -> Unit
            // Deleted here: delete there too (a deletion wins over a remote edit)
            l == null -> if (!r!!.deleted) softDelete += r.remoteId
            // Gone or deleted there: delete here, unless edited here since
            r == null || r.deleted -> if (localChanged) pushLocal(l, link.remoteId) else deleteLocal += l.localKey
            else -> {
                val remoteHash = hashOf(r.fields)
                val remoteChanged = remoteHash != link.hash
                when {
                    !localChanged && !remoteChanged -> newLinks += link
                    remoteHash == hashOf(l.fields)  -> newLinks += link.copy(hash = remoteHash)
                    localChanged                    -> pushLocal(l, link.remoteId)
                    else                            -> {
                        updateLocal[l.localKey] = r
                        newLinks += link.copy(hash = remoteHash)
                    }
                }
            }
        }
    }

    local.filter { it.localKey !in linkedLocal }.forEach { item ->
        val remoteId = newRemoteId(item)
        linkedLocal += item.localKey
        linkedRemote += remoteId
        // Same key created on both sides (e.g. a category for the same name): this device wins
        pushLocal(item, remoteId)
    }

    remote.filter { it.remoteId !in linkedRemote && !it.deleted }.forEach { insertLocal += it }

    return EntityPlan(push, softDelete, insertLocal, updateLocal, deleteLocal, newLinks)
}
