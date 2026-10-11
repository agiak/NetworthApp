package com.agcoding.networkapp.cloud.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPlannerTest {

    private fun local(key: String, name: String) = LocalItem(key, mapOf("name" to name))
    private fun remote(id: String, name: String, deleted: Boolean = false) = RemoteItem(id, mapOf("name" to name), deleted)
    private fun link(key: String, id: String, name: String) = SyncLink(key, id, hashOf(mapOf("name" to name)))

    private var counter = 0
    private val newId: (LocalItem) -> String = { "new-${++counter}" }

    @Test
    fun `first sync pushes everything local and pulls everything remote`() {
        val plan = planSync(
            local = listOf(local("1", "Anastasis")),
            remote = listOf(remote("r-x", "Xristina")),
            links = emptyList(),
            newRemoteId = newId,
        )
        assertEquals(mapOf("new-1" to mapOf("name" to "Anastasis")), plan.push)
        assertEquals(listOf("r-x"), plan.insertLocal.map { it.remoteId })
        assertEquals(listOf(SyncLink("1", "new-1", hashOf(mapOf("name" to "Anastasis")))), plan.links)
    }

    @Test
    fun `nothing changed means nothing to do`() {
        val plan = planSync(listOf(local("1", "A")), listOf(remote("r1", "A")), listOf(link("1", "r1", "A")), newId)
        assertTrue(plan.push.isEmpty() && plan.insertLocal.isEmpty() && plan.updateLocal.isEmpty())
        assertTrue(plan.softDelete.isEmpty() && plan.deleteLocal.isEmpty())
        assertEquals(1, plan.links.size)
    }

    @Test
    fun `local edit is pushed, remote edit is pulled`() {
        val pushed = planSync(listOf(local("1", "B")), listOf(remote("r1", "A")), listOf(link("1", "r1", "A")), newId)
        assertEquals(mapOf("r1" to mapOf("name" to "B")), pushed.push)

        val pulled = planSync(listOf(local("1", "A")), listOf(remote("r1", "C")), listOf(link("1", "r1", "A")), newId)
        assertEquals("C", pulled.updateLocal["1"]!!.fields["name"])
        assertEquals(hashOf(mapOf("name" to "C")), pulled.links.single().hash)
    }

    @Test
    fun `edits on both sides keep this device's version`() {
        val plan = planSync(listOf(local("1", "Mine")), listOf(remote("r1", "Theirs")), listOf(link("1", "r1", "Old")), newId)
        assertEquals(mapOf("r1" to mapOf("name" to "Mine")), plan.push)
        assertTrue(plan.updateLocal.isEmpty())
    }

    @Test
    fun `same edit on both sides just updates the link`() {
        val plan = planSync(listOf(local("1", "Same")), listOf(remote("r1", "Same")), listOf(link("1", "r1", "Old")), newId)
        assertTrue(plan.push.isEmpty() && plan.updateLocal.isEmpty())
        assertEquals(hashOf(mapOf("name" to "Same")), plan.links.single().hash)
    }

    @Test
    fun `local deletion is soft deleted on the server`() {
        val plan = planSync(emptyList(), listOf(remote("r1", "A")), listOf(link("1", "r1", "A")), newId)
        assertEquals(listOf("r1"), plan.softDelete)
        assertTrue(plan.links.isEmpty())
    }

    @Test
    fun `remote deletion deletes locally unless edited here`() {
        val deleted = planSync(listOf(local("1", "A")), listOf(remote("r1", "A", deleted = true)), listOf(link("1", "r1", "A")), newId)
        assertEquals(listOf("1"), deleted.deleteLocal)

        val kept = planSync(listOf(local("1", "Edited")), listOf(remote("r1", "A", deleted = true)), listOf(link("1", "r1", "A")), newId)
        assertTrue(kept.deleteLocal.isEmpty())
        assertEquals(mapOf("r1" to mapOf("name" to "Edited")), kept.push)
    }

    @Test
    fun `deleted remote rows nobody has are ignored`() {
        val plan = planSync(emptyList(), listOf(remote("r1", "A", deleted = true)), emptyList(), newId)
        assertTrue(plan.insertLocal.isEmpty())
    }

    @Test
    fun `unconfirmed push is retried with the same id`() {
        // Pushed last time but the app stopped before the server answered
        val missing = planSync(listOf(local("1", "A")), emptyList(), listOf(SyncLink("1", "r1", "")), newId)
        assertEquals(setOf("r1"), missing.push.keys)

        // The push did reach the server
        val arrived = planSync(listOf(local("1", "A")), listOf(remote("r1", "A")), listOf(SyncLink("1", "r1", "")), newId)
        assertTrue(arrived.push.isEmpty() && arrived.insertLocal.isEmpty())
        assertEquals(hashOf(mapOf("name" to "A")), arrived.links.single().hash)
    }

    @Test
    fun `same natural key on both sides is not duplicated`() {
        // Category feedback uses the name as id on both devices
        val plan = planSync(
            local = listOf(LocalItem("gym", mapOf("category" to "HEALTH"))),
            remote = listOf(RemoteItem("gym", mapOf("category" to "SUBSCRIPTIONS"))),
            links = emptyList(),
            newRemoteId = { it.localKey },
        )
        assertEquals(mapOf("gym" to mapOf("category" to "HEALTH")), plan.push)
        assertTrue(plan.insertLocal.isEmpty())
    }

    @Test
    fun `money and id lists are formatted the same on both sides`() {
        assertEquals("48.33", SyncFields.money(48.333))
        assertEquals("78410.00", SyncFields.money(78410.0))
        assertEquals("78410.00", SyncFields.money(java.math.BigDecimal("78410")))
        assertEquals("a,b", SyncFields.idList(listOf("b", "a", "", "b")))
    }
}
