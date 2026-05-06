package com.agcoding.networkapp.settings.domain.model

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val targetAmount: Double = 0.0
)
