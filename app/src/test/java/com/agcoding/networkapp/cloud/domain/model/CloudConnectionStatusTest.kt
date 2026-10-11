package com.agcoding.networkapp.cloud.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudConnectionStatusTest {

    private val session = CloudSession("access", "refresh", expiresAt = 0, userId = "u", email = "a@b.c")
    private val household = Household("h", "Home", isOwner = true)

    private fun status(
        state: CloudState,
        sync: SyncStatus = SyncStatus.Idle,
        online: Boolean = true,
        pending: Boolean = false,
    ) = cloudConnectionStatus(state, sync, online, pending)

    @Test
    fun `not signed in is offline mode`() {
        val s = status(CloudState(), pending = true)
        assertEquals(CloudMode.OFFLINE_MODE, s.mode)
        assertFalse(s.isConnected)
        // Pending changes only matter once connected
        assertFalse(s.hasPendingChanges)
    }

    @Test
    fun `signed in without household still needs setup`() {
        assertEquals(CloudMode.SETUP_INCOMPLETE, status(CloudState(session = session)).mode)
    }

    @Test
    fun `connected phone is online, syncing, failing or without internet`() {
        val connected = CloudState(session = session, household = household, lastSyncAt = 42)
        assertEquals(CloudMode.ONLINE, status(connected).mode)
        assertEquals(42L, status(connected).lastSyncAt)
        assertEquals(CloudMode.SYNCING, status(connected, SyncStatus.Syncing).mode)
        assertEquals(CloudMode.SYNC_ERROR, status(connected, SyncStatus.Failed("x")).mode)

        val noInternet = status(connected, SyncStatus.Failed("x"), online = false, pending = true)
        assertEquals(CloudMode.NO_INTERNET, noInternet.mode)
        assertTrue(noInternet.hasPendingChanges)
        assertTrue(noInternet.isConnected)
    }
}
