package com.agcoding.networkapp.recap.presentation.components

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
fun RecapHighlightsGrid(
    bestMonthLabel: String,
    bestMonthValue: String,
    worstMonthLabel: String,
    worstMonthValue: String,
    avgMonthlyGrowth: String,
    biggestJump: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecapStatCard(
                label = stringResource(R.string.recap_best_month),
                value = bestMonthValue,
                subLabel = bestMonthLabel,
                valueColor = PositiveGreen,
                modifier = Modifier.weight(1f)
            )
            RecapStatCard(
                label = stringResource(R.string.recap_worst_month),
                value = worstMonthValue,
                subLabel = worstMonthLabel,
                valueColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecapStatCard(
                label = stringResource(R.string.recap_avg_monthly),
                value = avgMonthlyGrowth,
                modifier = Modifier.weight(1f)
            )
            RecapStatCard(
                label = stringResource(R.string.recap_biggest_jump),
                value = biggestJump,
                valueColor = PositiveGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RecapStatCard(
    label: String,
    value: String,
    subLabel: String = "",
    valueColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
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
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor
            )
            if (subLabel.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecapHighlightsGridPreview() {
    NetWorthTheme {
        RecapHighlightsGrid(
            bestMonthLabel = "Mar",
            bestMonthValue = "+€3,200",
            worstMonthLabel = "Aug",
            worstMonthValue = "-€800",
            avgMonthlyGrowth = "+€1,250",
            biggestJump = "+€3,200"
        )
    }
}
