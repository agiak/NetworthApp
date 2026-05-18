package com.agcoding.networkapp.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.home.presentation.model.InsightData
import com.agcoding.networkapp.shared.ui.theme.IndicatorBlue
import com.agcoding.networkapp.shared.ui.theme.IndicatorOrange
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreenLight
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsCard(
    insight: InsightData,
    currencySymbol: String = "€",
    modifier: Modifier = Modifier
) {
    val bgColor: Color
    val icon: String
    val title: String
    val description: String

    when (insight) {
        is InsightData.GrowthStreak -> {
            bgColor = PositiveGreenLight
            icon = "✨"
            title = stringResource(R.string.insight_growth_title)
            description = stringResource(R.string.insight_growth_desc, insight.streakMonths)
        }
        is InsightData.BestMonth -> {
            val monthName = insight.yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
            bgColor = IndicatorOrange
            icon = "⚡"
            title = stringResource(R.string.insight_best_month_title, monthName)
            description = stringResource(
                R.string.insight_best_month_desc,
                "$currencySymbol${String.format(Locale.US, "%,.0f", insight.growthAmount)}"
            )
        }
        is InsightData.Forecast -> {
            val monthName = insight.targetYearMonth.month.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
            bgColor = IndicatorBlue
            icon = "🎯"
            title = stringResource(
                R.string.insight_forecast_title,
                "$currencySymbol${String.format(Locale.US, "%,.0f", insight.targetAmount)}",
                monthName
            )
            description = stringResource(R.string.insight_forecast_desc)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InsightsCardGrowthPreview() {
    NetWorthTheme {
        InsightsCard(insight = InsightData.GrowthStreak(streakMonths = 11))
    }
}

@Preview(showBackground = true)
@Composable
private fun InsightsCardBestMonthPreview() {
    NetWorthTheme {
        InsightsCard(insight = InsightData.BestMonth(yearMonth = YearMonth.of(2026, 3), growthAmount = 1360.0))
    }
}

@Preview(showBackground = true)
@Composable
private fun InsightsCardForecastPreview() {
    NetWorthTheme {
        InsightsCard(insight = InsightData.Forecast(targetAmount = 25000.0, targetYearMonth = YearMonth.of(2026, 10)))
    }
}
