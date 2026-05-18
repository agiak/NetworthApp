package com.agcoding.networkapp.shared.data.preferences

import android.content.Context
import com.agcoding.networkapp.shared.domain.preferences.ThemePreferencesRepository
import com.agcoding.networkapp.shared.ui.theme.AppThemeVariant
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ThemePreferencesRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _variant = MutableStateFlow(readVariant())

    override fun observeVariant(): Flow<AppThemeVariant> = _variant

    override suspend fun setVariant(variant: AppThemeVariant) {
        prefs.edit().putString(KEY_PALETTE, variant.name).apply()
        _variant.value = variant
    }

    private fun readVariant(): AppThemeVariant {
        val name = prefs.getString(KEY_PALETTE, AppThemeVariant.Default.name) ?: AppThemeVariant.Default.name
        return AppThemeVariant.entries.firstOrNull { it.name == name } ?: AppThemeVariant.Default
    }

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_PALETTE = "theme_palette"
    }
}
