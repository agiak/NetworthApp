package com.agcoding.networkapp.analytics.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.analytics.presentation.model.MonthlyEntryUiModel
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

/**
 * Shows a month-to-month transition row.
 * [transitionLabel] = "May → Jun"
 * [rangeLabel]      = "€10,000 → €10,420"
 */
@Composable
fun MonthByMonthRow(
    entry: MonthlyEntryUiModel,
    transitionLabel: String = "",
    rangeLabel: String = "",
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (entry.isFirst) return

    val negativeColor = MaterialTheme.colorScheme.error
    val diffColor = if (entry.isPositive) PositiveGreen else negativeColor
    val bgColor   = if (entry.isPositive) PositiveGreen.copy(alpha = 0.12f)
                    else negativeColor.copy(alpha = 0.10f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Direction circle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (entry.isPositive) Icons.Default.KeyboardArrowUp
                                  else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = diffColor,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Transition text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transitionLabel.ifEmpty { entry.monthLabel },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (rangeLabel.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = rangeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Change column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.formattedDiff,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = diffColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.formattedPercent,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                entry = MonthlyEntryUiModel("June 2026", "€10,420", "+€420", "+4.2%", true, false),
                transitionLabel = "May → Jun",
                rangeLabel = "€10,000 → €10,420",
            )
            MonthByMonthRow(
                entry = MonthlyEntryUiModel("July 2026", "€10,180", "€-240", "-2.3%", false, false),
                transitionLabel = "Jun → Jul",
                rangeLabel = "€10,420 → €10,180",
                showDivider = false
            )
        }
    }
}
