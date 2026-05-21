package com.agcoding.networkapp.fixedexpenses.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fixed_expenses")
data class FixedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val cost: Double,
    val dateEpochDay: Long? = null,
    @ColumnInfo(defaultValue = "MONTHLY") val recurrence: String = "MONTHLY",
    @ColumnInfo(defaultValue = "") val accountIds: String = "",
)
