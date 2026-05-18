package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class MarkOnboardingSeenUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : MarkOnboardingSeenUseCase {
    override suspend operator fun invoke() = repository.markOnboardingSeen()
}
