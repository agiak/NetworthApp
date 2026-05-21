package com.agcoding.networkapp.shared.di

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings_prefs",
    produceMigrations = { context -> listOf(sharedPrefsMigration(context, "settings_prefs")) },
)

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_prefs",
    produceMigrations = { context -> listOf(sharedPrefsMigration(context, "theme_prefs")) },
)

private val Context.biometricDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "biometric_prefs",
    produceMigrations = { context -> listOf(sharedPrefsMigration(context, "biometric_prefs")) },
)

private fun sharedPrefsMigration(context: Context, prefsName: String): DataMigration<Preferences> =
    object : DataMigration<Preferences> {
        private val sharedPrefs by lazy {
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        }

        override suspend fun shouldMigrate(currentData: Preferences): Boolean =
            sharedPrefs.all.isNotEmpty()

        override suspend fun migrate(currentData: Preferences): Preferences =
            currentData.toMutablePreferences().apply {
                sharedPrefs.all.forEach { (key, value) ->
                    when (value) {
                        is String  -> this[stringPreferencesKey(key)]  = value
                        is Boolean -> this[booleanPreferencesKey(key)] = value
                        is Float   -> this[floatPreferencesKey(key)]   = value
                        is Long    -> this[longPreferencesKey(key)]    = value
                        is Int     -> this[intPreferencesKey(key)]     = value
                    }
                }
            }.toPreferences()

        override suspend fun cleanUp() {
            sharedPrefs.edit().clear().apply()
        }
    }

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
