package com.agcoding.networkapp.settings.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Color.Gray,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
