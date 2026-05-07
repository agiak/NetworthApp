package com.agcoding.networkapp.analytics.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun TrendCard(
    trendLabel: String,
    trendDescription: String,
    isPositive: Boolean,
    isNeutral: Boolean,
    modifier: Modifier = Modifier
) {
    val trendColor = when {
        isPositive -> PositiveGreen
        isNeutral -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.analytics_trend).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = trendLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = trendColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = trendDescription,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                isPositive -> Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = trendColor,
                    modifier = Modifier.size(40.dp)
                )
                !isNeutral -> Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = trendColor,
                    modifier = Modifier.size(40.dp)
                )
                else -> Text(
                    text = "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = trendColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrendCardIncreasingPreview() {
    NetWorthTheme {
        TrendCard(
            trendLabel = "Increasing",
            trendDescription = "Net worth trending upward in this period",
            isPositive = true,
            isNeutral = false
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrendCardStablePreview() {
    NetWorthTheme {
        TrendCard(
            trendLabel = "Stable",
            trendDescription = "Net worth relatively stable in this period",
            isPositive = false,
            isNeutral = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrendCardDecreasingPreview() {
    NetWorthTheme {
        TrendCard(
            trendLabel = "Decreasing",
            trendDescription = "Net worth declining in this period",
            isPositive = false,
            isNeutral = false
        )
    }
}
