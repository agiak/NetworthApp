package com.agcoding.networkapp.shared.ui.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDimens(
    val spacing: Spacing = Spacing(),
    val elevation: Elevation = Elevation(),
) {
    data class Spacing(
        val xs: Dp = 4.dp, val sm: Dp = 8.dp,  val md: Dp = 16.dp,
        val lg: Dp = 24.dp, val xl: Dp = 32.dp, val xxl: Dp = 48.dp,
    )
    data class Elevation(val card: Dp = 2.dp, val dialog: Dp = 8.dp, val sheet: Dp = 16.dp)
}
