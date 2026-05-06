package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppThemeUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : GetAppThemeUseCase {
    override operator fun invoke(): Flow<AppTheme> = repository.getAppTheme()
}
