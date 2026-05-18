package com.agcoding.networkapp.shared.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.agcoding.networkapp.shared.ui.tokens.AppColorScheme
import com.agcoding.networkapp.shared.ui.tokens.AppDimens

val LocalAppColorScheme = staticCompositionLocalOf<AppColorScheme> {
    error("Wrap root with AppTheme")
}

val LocalAppDimens = staticCompositionLocalOf<AppDimens> {
    error("Wrap root with AppTheme")
}
