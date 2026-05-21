package com.agcoding.networkapp.fixedexpenses.presentation.model

data class AccountExpenseStatsUiModel(
    val accountId: Long,
    val accountName: String,
    val accountColorHex: String,
    val count: Int,
    val formattedMonthlyTotal: String,
)
