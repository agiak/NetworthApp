package com.agcoding.networkapp.settings.domain.usecase

import kotlinx.coroutines.flow.Flow

interface HasSeenOnboardingUseCase {
    operator fun invoke(): Flow<Boolean>
}
