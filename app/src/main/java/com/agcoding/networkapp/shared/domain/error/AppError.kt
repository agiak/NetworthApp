package com.agcoding.networkapp.shared.domain.error

sealed class AppError(override val message: String) : Exception(message) {
    data class DatabaseError(override val message: String) : AppError(message)
    data class NetworkError(override val message: String) : AppError(message)
    data class UnknownError(override val message: String = "An unknown error occurred") : AppError(message)
}
