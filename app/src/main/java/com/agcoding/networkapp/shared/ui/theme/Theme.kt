package com.agcoding.networkapp.shared.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = GreenGrey80,
    background = DarkBackground,
    surface = Color(0xFF1E2732),
    onPrimary = DarkBackground,
    onSurface = Color.White,
    onSurfaceVariant = Color.Gray,
    surfaceVariant = Color(0xFF1B2633),
    secondaryContainer = Color(0xFF1A3326),
    onSecondaryContainer = Green80
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = GreenGrey40,
    background = AppBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    surfaceVariant = Color(0xFFF9F6F2),
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = Green40
)

@Composable
fun NetWorthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NetWorthTypography,
        content = content
    )
}
