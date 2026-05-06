package com.agcoding.networkapp.settings.domain.usecase

interface GenerateDummyDataUseCase {
    suspend operator fun invoke(): Result<Unit>
}
