package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class SetAppCurrencyUseCaseImpl @Inject constructor(
    private val repository: SettingsRepository
) : SetAppCurrencyUseCase {
    override suspend operator fun invoke(currency: AppCurrency) = repository.setAppCurrency(currency)
}
