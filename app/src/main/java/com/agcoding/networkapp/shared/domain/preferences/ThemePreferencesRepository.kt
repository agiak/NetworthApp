package com.agcoding.networkapp.shared.domain.preferences

import com.agcoding.networkapp.shared.ui.theme.AppThemeVariant
import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    fun observeVariant(): Flow<AppThemeVariant>
    suspend fun setVariant(variant: AppThemeVariant)
}
