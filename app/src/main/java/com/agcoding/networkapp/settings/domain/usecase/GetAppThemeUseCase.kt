package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface GetAppThemeUseCase {
    operator fun invoke(): Flow<AppTheme>
}
