package com.agcoding.networkapp.settings.domain.model

import java.time.LocalDate

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val targetAmount: Double = 0.0,
    val createdAt: LocalDate? = null
)
