package com.agcoding.networkapp.history.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun MonthGroupHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun MonthGroupHeaderPreview() {
    NetWorthTheme {
        MonthGroupHeader(title = "March 2026")
    }
}
