package com.agcoding.networkapp.shared.ui.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.agcoding.networkapp.shared.ui.tokens.brand.BrandTokens

data class AppColorScheme(
    val backgroundPrimary: Color, val backgroundSecondary: Color,
    val backgroundCard: Color, val backgroundOverlay: Color,
    val contentPrimary: Color, val contentSecondary: Color, val contentDisabled: Color,
    val actionPrimary: Color, val actionPrimaryHover: Color,
    val actionContent: Color, val actionSecondary: Color, val actionSecondaryContent: Color,
    val borderDefault: Color, val borderFocused: Color, val borderStrong: Color,
    val statusSuccess: Color, val statusSuccessSubtle: Color,
    val statusError: Color, val statusErrorSubtle: Color,
    val statusWarning: Color, val statusWarningSubtle: Color,
)

fun buildColorScheme(brand: BrandTokens, isDark: Boolean): AppColorScheme =
    if (isDark) darkScheme(brand) else lightScheme(brand)

private fun lightScheme(b: BrandTokens) = AppColorScheme(
    backgroundPrimary      = b.neutral50,
    backgroundSecondary    = b.neutral100,
    backgroundCard         = Color.White,
    backgroundOverlay      = Color.Black.copy(alpha = 0.4f),
    contentPrimary         = b.neutral900,
    contentSecondary       = b.neutral600,
    contentDisabled        = b.neutral400,
    actionPrimary          = b.primary400,
    actionPrimaryHover     = b.primary600,
    actionContent          = Color.White,
    actionSecondary        = b.primary50,
    actionSecondaryContent = b.primary600,
    borderDefault          = b.neutral200,
    borderFocused          = b.primary400,
    borderStrong           = b.neutral600,
    statusSuccess          = b.success400,
    statusSuccessSubtle    = b.success400.copy(alpha = 0.12f),
    statusError            = b.error400,
    statusErrorSubtle      = b.error400.copy(alpha = 0.12f),
    statusWarning          = b.warning400,
    statusWarningSubtle    = b.warning400.copy(alpha = 0.12f),
)

private fun darkScheme(b: BrandTokens) = AppColorScheme(
    backgroundPrimary      = b.neutral900,
    // Solid interpolated colors — avoids transparency issues with ModalBottomSheet / surfaces
    backgroundSecondary    = lerp(b.neutral900, b.neutral600, 0.45f),
    backgroundCard         = lerp(b.neutral900, b.neutral200, 0.12f),
    backgroundOverlay      = Color.Black.copy(alpha = 0.6f),
    contentPrimary         = b.neutral50,
    contentSecondary       = b.neutral200,
    contentDisabled        = b.neutral400,
    actionPrimary          = b.primary400,
    actionPrimaryHover     = b.primary200,
    actionContent          = Color.White,
    actionSecondary        = b.primary800,
    actionSecondaryContent = b.primary200,
    borderDefault          = lerp(b.neutral900, b.neutral600, 0.40f),
    borderFocused          = b.primary400,
    borderStrong           = b.neutral400,
    statusSuccess          = b.success300,
    statusSuccessSubtle    = b.success300.copy(alpha = 0.15f),
    statusError            = b.error300,
    statusErrorSubtle      = b.error300.copy(alpha = 0.15f),
    statusWarning          = b.warning300,
    statusWarningSubtle    = b.warning300.copy(alpha = 0.15f),
)

fun AppColorScheme.toMaterial3ColorScheme(): ColorScheme {
    val dark = backgroundPrimary.luminance() < 0.5f
    return if (dark) {
        darkColorScheme(
            primary          = actionPrimary,
            onPrimary        = actionContent,
            background       = backgroundPrimary,
            onBackground     = contentPrimary,
            surface          = backgroundCard,
            onSurface        = contentPrimary,
            surfaceVariant   = backgroundSecondary,
            onSurfaceVariant = contentSecondary,
            error            = statusError,
            onError          = actionContent,
        )
    } else {
        lightColorScheme(
            primary          = actionPrimary,
            onPrimary        = actionContent,
            background       = backgroundPrimary,
            onBackground     = contentPrimary,
            surface          = backgroundCard,
            onSurface        = contentPrimary,
            surfaceVariant   = backgroundSecondary,
            onSurfaceVariant = contentSecondary,
            error            = statusError,
            onError          = actionContent,
        )
    }
}
