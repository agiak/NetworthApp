package com.agcoding.networkapp.fixedexpenses.presentation.model

import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType

data class FixedExpenseUiModel(
    val id: Long,
    val title: String,
    val note: String,
    val formattedCost: String,
    val costRaw: Double,
    val formattedDate: String?,
    val recurrence: RecurrenceType,
    val monthlyEquivalent: String?,
    val accountIds: List<Long> = emptyList(),
    val accountColors: List<String> = emptyList(),
    /** Set only when an account filter is active and the expense is shared: the filtered accounts' share (1–99) */
    val sharePercent: Int? = null,
    /** Full cost of a shared expense, shown next to [sharePercent] */
    val formattedFullCost: String? = null,
)
