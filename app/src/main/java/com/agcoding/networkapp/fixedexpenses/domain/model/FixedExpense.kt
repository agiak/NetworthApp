package com.agcoding.networkapp.fixedexpenses.domain.model

import java.time.LocalDate

data class FixedExpense(
    val id: Long = 0,
    val title: String,
    val note: String = "",
    val cost: Double,
    val date: LocalDate? = null,
    val recurrence: RecurrenceType = RecurrenceType.MONTHLY,
)
