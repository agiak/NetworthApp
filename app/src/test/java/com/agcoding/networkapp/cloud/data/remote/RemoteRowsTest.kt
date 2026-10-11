package com.agcoding.networkapp.cloud.data.remote

import com.agcoding.networkapp.cloud.domain.sync.SyncFields
import com.agcoding.networkapp.cloud.domain.sync.hashOf
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteRowsTest {

    @Test
    fun `server rows become the same fields the phone produces`() {
        // As PostgREST returns them: numeric as numbers, arrays as JSON arrays
        val expense = JSONObject("""
            {"id":"e1","household_id":"h","title":"Car insurance","note":"","cost":600,"expense_date":null,
             "recurrence":"ANNUAL","account_ids":["b","a"],"deleted_at":null}
        """)
        val item = RemoteRows.parse(RemoteTable.EXPENSES, expense)
        assertEquals("e1", item.remoteId)
        assertFalse(item.deleted)
        val phone = mapOf(
            SyncFields.Expense.TITLE to "Car insurance",
            SyncFields.Expense.NOTE to "",
            SyncFields.Expense.COST to SyncFields.money(600.0),
            SyncFields.Expense.DATE to null,
            SyncFields.Expense.RECURRENCE to "ANNUAL",
            SyncFields.Expense.ACCOUNT_IDS to SyncFields.idList(listOf("a", "b")),
        )
        assertEquals(phone, item.fields)
        assertEquals(hashOf(phone), hashOf(item.fields))
    }

    @Test
    fun `fields go back to the server as typed json`() {
        val fields = mapOf(
            SyncFields.Expense.TITLE to "House",
            SyncFields.Expense.NOTE to "",
            SyncFields.Expense.COST to "350.00",
            SyncFields.Expense.DATE to null,
            SyncFields.Expense.RECURRENCE to "MONTHLY",
            SyncFields.Expense.ACCOUNT_IDS to "",
        )
        val json = RemoteRows.toJson(RemoteTable.EXPENSES, "h", "e1", fields)
        assertEquals("e1", json.getString("id"))
        assertEquals("h", json.getString("household_id"))
        assertTrue(json.isNull("deleted_at"))
        assertTrue(json.isNull("expense_date"))
        assertEquals(350.0, json.getDouble("cost"), 0.0)
        assertEquals(0, json.getJSONArray("account_ids").length())
        // And reading it back gives the same fields
        assertEquals(fields, RemoteRows.parse(RemoteTable.EXPENSES, JSONObject(json.toString())).fields)
    }

    @Test
    fun `entries, accounts and categories round trip`() {
        val entry = mapOf("account_id" to "a1", "value" to "78410.00", "entry_date" to "2026-10-01", "note" to "")
        assertEquals(entry, RemoteRows.parse(RemoteTable.ENTRIES, RemoteRows.toJson(RemoteTable.ENTRIES, "h", "n1", entry)).fields)

        val account = mapOf("name" to "Xristina", "starting_balance" to "0.00", "color_hex" to "#A78BFA", "monthly_salary" to "1500.00")
        assertEquals(account, RemoteRows.parse(RemoteTable.ACCOUNTS, RemoteRows.toJson(RemoteTable.ACCOUNTS, "h", "a1", account)).fields)

        val feedback = RemoteRows.toJson(RemoteTable.FEEDBACK, "h", "gym", mapOf("category" to "SUBSCRIPTIONS"))
        assertEquals("gym", feedback.getString("name_key"))
        assertEquals("household_id,name_key", RemoteTable.FEEDBACK.conflictColumns)
        assertEquals("gym", RemoteRows.parse(RemoteTable.FEEDBACK, feedback).remoteId)
    }

    @Test
    fun `deleted rows are flagged`() {
        val row = JSONObject("""{"name_key":"x","category":"OTHER","deleted_at":"2026-10-11T10:00:00Z"}""")
        assertTrue(RemoteRows.parse(RemoteTable.FEEDBACK, row).deleted)
    }

    @Test
    fun `errors from auth and database are understood`() {
        val auth = SupabaseClient.parseError(400, """{"code":400,"error_code":"invalid_credentials","msg":"Invalid login credentials"}""")
        assertEquals("invalid_credentials", auth.code)
        assertEquals("Invalid login credentials", auth.message)

        val db = SupabaseClient.parseError(400, """{"code":"P0002","details":null,"hint":null,"message":"Invite code is invalid or expired"}""")
        assertEquals("P0002", db.code)

        val plain = SupabaseClient.parseError(502, "Bad gateway")
        assertNull(plain.code)
        assertEquals("Bad gateway", plain.message)
    }
}
