package com.agcoding.networkapp.home.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "net_worth_entries")
data class NetWorthEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val value: Double,
    val dateEpochDay: Long
)
