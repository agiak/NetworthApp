package com.agcoding.networkapp.biometric.data.di

import com.agcoding.networkapp.biometric.data.repository.BiometricRepositoryImpl
import com.agcoding.networkapp.biometric.domain.repository.BiometricRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BiometricModule {

    @Binds
    @Singleton
    abstract fun bindBiometricRepository(impl: BiometricRepositoryImpl): BiometricRepository
}
