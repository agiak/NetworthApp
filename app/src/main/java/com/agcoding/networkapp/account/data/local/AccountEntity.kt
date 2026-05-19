package com.agcoding.networkapp.account.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startingBalance: Double = 0.0,
    val colorHex: String = "#76C893",
)
