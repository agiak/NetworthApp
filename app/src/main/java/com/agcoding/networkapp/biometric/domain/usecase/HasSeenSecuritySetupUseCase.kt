package com.agcoding.networkapp.biometric.domain.usecase

import com.agcoding.networkapp.biometric.domain.repository.BiometricRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HasSeenSecuritySetupUseCase @Inject constructor(
    private val repository: BiometricRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.hasSeenSecuritySetup()
}
