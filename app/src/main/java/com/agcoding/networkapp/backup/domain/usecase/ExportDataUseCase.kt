package com.agcoding.networkapp.backup.domain.usecase

interface ExportDataUseCase {
    suspend operator fun invoke(): String
}
