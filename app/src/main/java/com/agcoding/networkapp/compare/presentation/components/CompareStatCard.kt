package com.agcoding.networkapp.compare.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

private val PreviousColor = Color(0xFF8B7FD4)

@Composable
fun CompareStatCard(
    label: String,
    previousLabel: String,
    previousValue: String,
    previousPositive: Boolean,
    currentLabel: String,
    currentValue: String,
    currentPositive: Boolean,
    diff: String,
    improved: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PeriodColumn(
                    periodLabel = previousLabel,
                    value = previousValue,
                    valueColor = if (previousPositive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    labelColor = PreviousColor.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
                PeriodColumn(
                    periodLabel = currentLabel,
                    value = currentValue,
                    valueColor = if (currentPositive) PositiveGreen else MaterialTheme.colorScheme.error,
                    labelColor = PositiveGreen.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                    highlight = true
                )
            }

            if (diff.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val diffColor = if (improved) PositiveGreen else MaterialTheme.colorScheme.error
                    Icon(
                        imageVector = if (improved) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = diffColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val diffText = if (improved)
                        stringResource(R.string.compare_diff_more, diff)
                    else
                        stringResource(R.string.compare_diff_less, diff)
                    Text(
                        text = diffText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = diffColor
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodColumn(
    periodLabel: String,
    value: String,
    valueColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = periodLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompareStatCardPreview() {
    NetWorthTheme {
        CompareStatCard(
            label = "Total Growth",
            previousLabel = "2025",
            previousValue = "+€12,000",
            previousPositive = true,
            currentLabel = "2026",
            currentValue = "+€18,400",
            currentPositive = true,
            diff = "€6,400",
            improved = true
        )
    }
}
