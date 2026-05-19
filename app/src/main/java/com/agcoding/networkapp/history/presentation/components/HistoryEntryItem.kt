package com.agcoding.networkapp.history.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.shared.ui.model.EntryUiModel
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun HistoryEntryItem(
    entry: EntryUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dotColor = if (entry.accountColorHex.isNotEmpty()) {
        try { Color(android.graphics.Color.parseColor(entry.accountColorHex)) }
        catch (e: Exception) { null }
    } else null

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dotColor != null) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = entry.formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = entry.formattedValue,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun SwipeDeleteBackground(
    modifier: Modifier = Modifier,
    tintColor: androidx.compose.ui.graphics.Color = LocalAppColorScheme.current.statusError,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(tintColor),
        contentAlignment = Alignment.CenterEnd,
    ) {
        if (tintColor != androidx.compose.ui.graphics.Color.Transparent) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(end = 20.dp).size(22.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryEntryItemPreview() {
    NetWorthTheme {
        HistoryEntryItem(
            entry = EntryUiModel(1L, "20 Mar", "€16,500", accountColorHex = "#5B8DEF"),
            onClick = {},
        )
    }
}
