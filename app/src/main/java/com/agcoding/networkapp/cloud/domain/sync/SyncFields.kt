package com.agcoding.networkapp.cloud.domain.sync

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Field names (the server's column names) and value formats shared by both sides,
 * so that the same row hashes the same on the phone and on the server.
 */
object SyncFields {

    object Account {
        const val NAME = "name"
        const val STARTING_BALANCE = "starting_balance"
        const val COLOR_HEX = "color_hex"
        const val MONTHLY_SALARY = "monthly_salary"
    }

    object Entry {
        const val ACCOUNT_ID = "account_id"
        const val VALUE = "value"
        const val DATE = "entry_date"
        const val NOTE = "note"
    }

    object Expense {
        const val TITLE = "title"
        const val NOTE = "note"
        const val COST = "cost"
        const val DATE = "expense_date"
        const val RECURRENCE = "recurrence"
        const val ACCOUNT_IDS = "account_ids"
    }

    object Feedback {
        const val CATEGORY = "category"
    }

    /** Columns sent as JSON numbers. */
    val NUMERIC = setOf(Account.STARTING_BALANCE, Account.MONTHLY_SALARY, Entry.VALUE, Expense.COST)

    /** Columns sent as JSON arrays (stored here as comma-separated ids). */
    val ARRAYS = setOf(Expense.ACCOUNT_IDS)

    /** Money as the server stores it: two decimals. */
    fun money(value: Double): String = money(BigDecimal.valueOf(value))

    fun money(value: BigDecimal): String = value.setScale(2, RoundingMode.HALF_UP).toPlainString()

    /** Ids in a stable order, comma-separated. */
    fun idList(ids: Collection<String>): String = ids.filter { it.isNotBlank() }.distinct().sorted().joinToString(",")
}
