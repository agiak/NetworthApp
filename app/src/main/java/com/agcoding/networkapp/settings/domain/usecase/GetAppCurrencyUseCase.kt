package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.settings.domain.model.AppCurrency
import kotlinx.coroutines.flow.Flow

interface GetAppCurrencyUseCase {
    operator fun invoke(): Flow<AppCurrency>
}
