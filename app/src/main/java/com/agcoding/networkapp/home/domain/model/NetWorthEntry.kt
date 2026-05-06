package com.agcoding.networkapp.home.domain.model

import java.time.LocalDate

data class NetWorthEntry(
    val id: Long = 0,
    val value: Double,
    val date: LocalDate
)
