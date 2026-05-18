package com.agcoding.networkapp.shared.ui.theme

import com.agcoding.networkapp.shared.ui.tokens.brand.BrandTokens
import com.agcoding.networkapp.shared.ui.tokens.brand.DefaultBrandTokens
import com.agcoding.networkapp.shared.ui.tokens.brand.OceanBrandTokens

enum class AppThemeVariant {
    Default, Ocean;

    fun toBrandTokens(): BrandTokens = when (this) {
        Default -> DefaultBrandTokens
        Ocean   -> OceanBrandTokens
    }
}
