package com.agcoding.networkapp.home.domain.usecase

interface DeleteNetWorthEntryUseCase {
    suspend operator fun invoke(id: Long): Result<Unit>
}
