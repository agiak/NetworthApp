package com.agcoding.networkapp.backup.domain.usecase

interface ExportCsvUseCase {
    suspend operator fun invoke(): String
}
