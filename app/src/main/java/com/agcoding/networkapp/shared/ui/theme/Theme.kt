package com.agcoding.networkapp.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.agcoding.networkapp.shared.ui.tokens.AppDimens
import com.agcoding.networkapp.shared.ui.tokens.AppShapes
import com.agcoding.networkapp.shared.ui.tokens.buildColorScheme
import com.agcoding.networkapp.shared.ui.tokens.toMaterial3ColorScheme

@Composable
fun AppTheme(
    variant: AppThemeVariant = AppThemeVariant.Default,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(variant, isDark) {
        buildColorScheme(variant.toBrandTokens(), isDark)
    }
    CompositionLocalProvider(
        LocalAppColorScheme provides colorScheme,
        LocalAppDimens      provides AppDimens(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme.toMaterial3ColorScheme(),
            typography  = NetWorthTypography,
            shapes      = AppShapes,
            content     = content,
        )
    }
}

@Composable
fun NetWorthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    AppTheme(isDark = darkTheme, content = content)
}
