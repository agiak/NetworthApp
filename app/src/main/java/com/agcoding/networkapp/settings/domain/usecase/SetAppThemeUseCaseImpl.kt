package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class SetAppThemeUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : SetAppThemeUseCase {
    override suspend operator fun invoke(theme: AppTheme) = repository.setAppTheme(theme)
}
