package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.AppCurrency

interface SetAppCurrencyUseCase {
    suspend operator fun invoke(currency: AppCurrency)
}
