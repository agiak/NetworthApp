package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppCurrencyUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : GetAppCurrencyUseCase {
    override operator fun invoke(): Flow<AppCurrency> = repository.getAppCurrency()
}
