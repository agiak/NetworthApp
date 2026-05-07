package com.agcoding.networkapp.analytics.presentation.prediction.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
fun PredictionScenarioCards(
    minimumValue: String,
    maximumValue: String,
    conservativeMonthlyRate: String,
    optimisticMonthlyRate: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScenarioCard(
            label = stringResource(R.string.prediction_minimum_label),
            value = minimumValue,
            description = stringResource(R.string.prediction_minimum_desc),
            monthlyRate = stringResource(R.string.prediction_monthly_rate, conservativeMonthlyRate),
            valueColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        ScenarioCard(
            label = stringResource(R.string.prediction_maximum_label),
            value = maximumValue,
            description = stringResource(R.string.prediction_maximum_desc),
            monthlyRate = stringResource(R.string.prediction_monthly_rate, optimisticMonthlyRate),
            valueColor = PositiveGreen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ScenarioCard(
    label: String,
    value: String,
    description: String,
    monthlyRate: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = monthlyRate,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PredictionScenarioCardsPreview() {
    NetWorthTheme {
        PredictionScenarioCards(
            minimumValue = "€45,200",
            maximumValue = "€98,000",
            conservativeMonthlyRate = "+€350",
            optimisticMonthlyRate = "+€950"
        )
    }
}
