package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppLanguageUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : GetAppLanguageUseCase {
    override operator fun invoke(): Flow<AppLanguage> = repository.getAppLanguage()
}
