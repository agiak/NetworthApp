package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HasSeenOnboardingUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : HasSeenOnboardingUseCase {
    override operator fun invoke(): Flow<Boolean> = repository.hasSeenOnboarding()
}
