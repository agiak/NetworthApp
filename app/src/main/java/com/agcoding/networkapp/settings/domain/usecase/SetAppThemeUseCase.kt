package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.AppTheme

interface SetAppThemeUseCase {
    suspend operator fun invoke(theme: AppTheme)
}
