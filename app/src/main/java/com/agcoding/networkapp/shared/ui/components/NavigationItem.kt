package com.agcoding.networkapp.shared.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.agcoding.networkapp.shared.navigation.Screen

data class NavigationItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)
