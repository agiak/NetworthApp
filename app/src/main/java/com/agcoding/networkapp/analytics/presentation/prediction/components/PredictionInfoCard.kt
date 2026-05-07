package com.agcoding.networkapp.analytics.presentation.prediction.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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

@Composable
fun PredictionInfoCard(
    avgMonthlyGrowth: String,
    conservativeMonthlyRate: String,
    optimisticMonthlyRate: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.prediction_info_title).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(
                scenarioLabel = stringResource(R.string.prediction_expected_label),
                labelColor = PositiveGreen,
                descriptionText = stringResource(R.string.prediction_info_expected, avgMonthlyGrowth)
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            InfoRow(
                scenarioLabel = stringResource(R.string.prediction_minimum_label),
                labelColor = MaterialTheme.colorScheme.onSurface,
                descriptionText = stringResource(R.string.prediction_info_conservative, conservativeMonthlyRate)
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            InfoRow(
                scenarioLabel = stringResource(R.string.prediction_maximum_label),
                labelColor = PositiveGreen.copy(alpha = 0.75f),
                descriptionText = stringResource(R.string.prediction_info_optimistic, optimisticMonthlyRate)
            )
        }
    }
}

@Composable
private fun InfoRow(
    scenarioLabel: String,
    labelColor: Color,
    descriptionText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = scenarioLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.62f),
            lineHeight = 17.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PredictionInfoCardPreview() {
    NetWorthTheme {
        PredictionInfoCard(
            avgMonthlyGrowth = "+€683",
            conservativeMonthlyRate = "+€350",
            optimisticMonthlyRate = "+€1,020"
        )
    }
}
