package com.agcoding.networkapp.shared.di

import com.agcoding.networkapp.shared.data.preferences.ThemePreferencesRepositoryImpl
import com.agcoding.networkapp.shared.domain.preferences.ThemePreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ThemePreferencesModule {

    @Binds
    @Singleton
    abstract fun bindThemePreferencesRepository(
        impl: ThemePreferencesRepositoryImpl,
    ): ThemePreferencesRepository
}
