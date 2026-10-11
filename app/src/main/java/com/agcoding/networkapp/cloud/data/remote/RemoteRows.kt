package com.agcoding.networkapp.cloud.data.remote

import com.agcoding.networkapp.cloud.domain.sync.RemoteItem
import com.agcoding.networkapp.cloud.domain.sync.SyncFields
import com.agcoding.networkapp.cloud.domain.sync.SyncFields.Account
import com.agcoding.networkapp.cloud.domain.sync.SyncFields.Entry
import com.agcoding.networkapp.cloud.domain.sync.SyncFields.Expense
import com.agcoding.networkapp.cloud.domain.sync.SyncFields.Feedback
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

/** The synced tables: their name, key column and how their rows map to sync fields. */
enum class RemoteTable(
    val tableName: String,
    /** Column that identifies a row within a household. */
    val keyColumn: String,
    val fieldNames: List<String>,
) {
    ACCOUNTS("accounts", "id", listOf(Account.NAME, Account.STARTING_BALANCE, Account.COLOR_HEX, Account.MONTHLY_SALARY)),
    ENTRIES("net_worth_entries", "id", listOf(Entry.ACCOUNT_ID, Entry.VALUE, Entry.DATE, Entry.NOTE)),
    EXPENSES("fixed_expenses", "id", listOf(Expense.TITLE, Expense.NOTE, Expense.COST, Expense.DATE, Expense.RECURRENCE, Expense.ACCOUNT_IDS)),
    FEEDBACK("category_feedback", "name_key", listOf(Feedback.CATEGORY));

    /** Rows are upserted on their key (category feedback's key is only unique per household). */
    val conflictColumns: String get() = if (keyColumn == "id") "id" else "household_id,$keyColumn"
}

object RemoteRows {

    fun parse(table: RemoteTable, row: JSONObject): RemoteItem = RemoteItem(
        remoteId = row.getString(table.keyColumn),
        fields   = table.fieldNames.associateWith { name -> readField(row, name) },
        deleted  = !row.isNull("deleted_at"),
    )

    /** A full row for an upsert, live (deleted_at cleared). */
    fun toJson(table: RemoteTable, householdId: String, remoteId: String, fields: Map<String, String?>): JSONObject {
        val row = JSONObject()
            .put(table.keyColumn, remoteId)
            .put("household_id", householdId)
            .put("deleted_at", JSONObject.NULL)
        table.fieldNames.forEach { name ->
            val value = fields[name]
            row.put(name, when {
                value == null                -> JSONObject.NULL
                name in SyncFields.NUMERIC   -> BigDecimal(value)
                name in SyncFields.ARRAYS    -> JSONArray(value.split(",").filter { it.isNotBlank() })
                else                         -> value
            })
        }
        return row
    }

    private fun readField(row: JSONObject, name: String): String? {
        if (row.isNull(name)) return null
        return when (name) {
            in SyncFields.NUMERIC -> SyncFields.money(BigDecimal(row.get(name).toString()))
            in SyncFields.ARRAYS  -> {
                val array = row.optJSONArray(name) ?: JSONArray()
                SyncFields.idList((0 until array.length()).map { array.getString(it) })
            }
            else -> row.get(name).toString()
        }
    }
}
