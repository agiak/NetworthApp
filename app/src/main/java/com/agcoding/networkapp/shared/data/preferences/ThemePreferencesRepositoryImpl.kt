package com.agcoding.networkapp.shared.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.agcoding.networkapp.shared.di.ThemeDataStore
import com.agcoding.networkapp.shared.domain.preferences.ThemePreferencesRepository
import com.agcoding.networkapp.shared.ui.theme.AppThemeVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferencesRepositoryImpl @Inject constructor(
    @ThemeDataStore private val dataStore: DataStore<Preferences>,
) : ThemePreferencesRepository {

    override fun observeVariant(): Flow<AppThemeVariant> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val name = prefs[KEY_PALETTE] ?: AppThemeVariant.Default.name
            AppThemeVariant.entries.firstOrNull { it.name == name } ?: AppThemeVariant.Default
        }

    override suspend fun setVariant(variant: AppThemeVariant) {
        dataStore.edit { prefs -> prefs[KEY_PALETTE] = variant.name }
    }

    private companion object {
        val KEY_PALETTE = stringPreferencesKey("theme_palette")
    }
}
