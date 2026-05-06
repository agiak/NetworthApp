package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class SetAppLanguageUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : SetAppLanguageUseCase {
    override suspend operator fun invoke(language: AppLanguage) = repository.setAppLanguage(language)
}
