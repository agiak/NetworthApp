package com.agcoding.networkapp.biometric.domain.usecase

import com.agcoding.networkapp.biometric.domain.repository.BiometricRepository
import javax.inject.Inject

class SetBiometricEnabledUseCase @Inject constructor(
    private val repository: BiometricRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setBiometricEnabled(enabled)
}
