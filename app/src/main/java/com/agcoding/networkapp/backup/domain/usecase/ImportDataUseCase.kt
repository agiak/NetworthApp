package com.agcoding.networkapp.backup.domain.usecase

interface ImportDataUseCase {
    fun validate(json: String): Boolean
    suspend operator fun invoke(json: String): Result<Unit>
}
