package com.agcoding.networkapp.recap.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.recap.presentation.MonthlyBreakdownItem
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun RecapMonthByMonth(
    items: List<MonthlyBreakdownItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.recap_month_by_month),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    MonthRow(item = item)
                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthRow(item: MonthlyBreakdownItem) {
    val changeColor = when {
        item.isFirst -> MaterialTheme.colorScheme.onSurfaceVariant
        item.isPositive -> PositiveGreen
        else -> MaterialTheme.colorScheme.error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.monthLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = item.formattedValue,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 12.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!item.isFirst) {
                Icon(
                    imageVector = if (item.isPositive) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = changeColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = item.formattedChange,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = changeColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecapMonthByMonthPreview() {
    NetWorthTheme {
        RecapMonthByMonth(
            items = listOf(
                MonthlyBreakdownItem("January", "€60,000", "+€2,000", true, false),
                MonthlyBreakdownItem("February", "€61,500", "+€1,500", true, false),
                MonthlyBreakdownItem("March", "€64,700", "+€3,200", true, false),
                MonthlyBreakdownItem("April", "€63,900", "-€800", false, false)
            )
        )
    }
}
