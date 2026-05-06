package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.AppLanguage

interface SetAppLanguageUseCase {
    suspend operator fun invoke(language: AppLanguage)
}
