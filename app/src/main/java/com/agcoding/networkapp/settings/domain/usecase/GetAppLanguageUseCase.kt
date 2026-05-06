package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow

interface GetAppLanguageUseCase {
    operator fun invoke(): Flow<AppLanguage>
}
