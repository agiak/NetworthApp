package com.agcoding.networkapp.shared.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings_prefs")
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore("theme_prefs")
private val Context.biometricDataStore: DataStore<Preferences> by preferencesDataStore("biometric_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @SettingsDataStore
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore

    @Provides
    @Singleton
    @ThemeDataStore
    fun provideThemeDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.themeDataStore

    @Provides
    @Singleton
    @BiometricDataStore
    fun provideBiometricDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.biometricDataStore
}
