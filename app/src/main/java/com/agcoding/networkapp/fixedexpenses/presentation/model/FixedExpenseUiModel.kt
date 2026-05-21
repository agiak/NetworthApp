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
)
