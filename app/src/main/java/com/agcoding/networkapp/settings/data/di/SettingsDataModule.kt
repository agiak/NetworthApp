package com.agcoding.networkapp.settings.data.di

import com.agcoding.networkapp.settings.data.repository.SettingsRepositoryImpl
import com.agcoding.networkapp.settings.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsDataModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
