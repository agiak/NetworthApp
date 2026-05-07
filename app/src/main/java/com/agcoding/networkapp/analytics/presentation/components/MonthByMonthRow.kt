package com.agcoding.networkapp.analytics.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.analytics.presentation.model.MonthlyEntryUiModel
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun MonthByMonthRow(
    entry: MonthlyEntryUiModel,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    val negativeColor = MaterialTheme.colorScheme.error
    val diffColor = when {
        entry.isFirst -> MaterialTheme.colorScheme.onSurfaceVariant
        entry.isPositive -> PositiveGreen
        else -> negativeColor
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.monthLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.formattedValue,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!entry.isFirst) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (entry.isPositive) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = diffColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${entry.formattedDiff}  ${entry.formattedPercent}",
                            style = MaterialTheme.typography.labelMedium,
                            color = diffColor
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.formattedDiff,
                        style = MaterialTheme.typography.labelMedium,
                        color = diffColor
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthByMonthRowPreview() {
    NetWorthTheme {
        Column {
            MonthByMonthRow(
                entry = MonthlyEntryUiModel("February 2026", "€23,000", "+€3,000", "+15.0%", true, false)
            )
            MonthByMonthRow(
                entry = MonthlyEntryUiModel("January 2026", "€20,000", "-€500", "-2.4%", false, false),
                showDivider = false
            )
        }
    }
}
