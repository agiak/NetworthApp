package com.agcoding.networkapp.home.domain.model

import java.time.LocalDate

data class NetWorthEntry(
    val id: Long = 0,
    val value: Double,
    val date: LocalDate,
    val note: String = "",
    val accountId: Long = 1,
) {
    companion object {
        const val DELETION_MARKER = "__account_deleted__"
    }
}
