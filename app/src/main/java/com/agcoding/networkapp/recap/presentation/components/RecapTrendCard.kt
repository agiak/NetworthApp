package com.agcoding.networkapp.recap.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.recap.presentation.RecapTrend
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun RecapTrendCard(
    trend: RecapTrend,
    modifier: Modifier = Modifier
) {
    val trendText = when (trend) {
        RecapTrend.ACCELERATED -> stringResource(R.string.recap_trend_accelerated)
        RecapTrend.STABLE -> stringResource(R.string.recap_trend_stable)
        RecapTrend.DECELERATED -> stringResource(R.string.recap_trend_decelerated)
        RecapTrend.DECLINED -> stringResource(R.string.recap_trend_declined)
    }
    val trendColor = when (trend) {
        RecapTrend.ACCELERATED, RecapTrend.STABLE -> PositiveGreen
        RecapTrend.DECELERATED -> MaterialTheme.colorScheme.onSurfaceVariant
        RecapTrend.DECLINED -> MaterialTheme.colorScheme.error
    }
    val trendEmoji = when (trend) {
        RecapTrend.ACCELERATED -> "🚀"
        RecapTrend.STABLE -> "📈"
        RecapTrend.DECELERATED -> "📊"
        RecapTrend.DECLINED -> "💪"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.recap_trend_title).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$trendEmoji  $trendText",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = trendColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecapTrendCardPreview() {
    NetWorthTheme {
        RecapTrendCard(trend = RecapTrend.ACCELERATED)
    }
}
