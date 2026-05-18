package com.agcoding.networkapp.biometric.domain.usecase

import com.agcoding.networkapp.biometric.domain.repository.BiometricRepository
import javax.inject.Inject

class SetPinUseCase @Inject constructor(
    private val repository: BiometricRepository,
) {
    suspend operator fun invoke(pin: String) = repository.setPin(pin)
}
