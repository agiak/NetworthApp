package com.agcoding.networkapp.shared.ui.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // chips, badges
    small      = RoundedCornerShape(8.dp),   // inputs
    medium     = RoundedCornerShape(12.dp),  // cards, dialogs
    large      = RoundedCornerShape(16.dp),  // bottom sheets
    extraLarge = RoundedCornerShape(24.dp),  // full-bleed cards
)
