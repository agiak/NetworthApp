package com.agcoding.networkapp.goal.presentation.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun GoalStatsCards(
    requiredMonthly: String,
    requiredYearly: String,
    avgMonthlyGrowth: String,
    yearsAtCurrentPace: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GoalStatCard(
                label = stringResource(R.string.goal_required_monthly),
                value = requiredMonthly,
                modifier = Modifier.weight(1f)
            )
            GoalStatCard(
                label = stringResource(R.string.goal_required_yearly),
                value = requiredYearly,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GoalStatCard(
                label = stringResource(R.string.goal_your_avg_monthly),
                value = avgMonthlyGrowth,
                valueColor = PositiveGreen,
                modifier = Modifier.weight(1f)
            )
            GoalStatCard(
                label = stringResource(R.string.goal_at_pace_label),
                value = yearsAtCurrentPace,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GoalStatCard(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (valueColor == androidx.compose.ui.graphics.Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalStatsCardsPreview() {
    NetWorthTheme {
        GoalStatsCards(
            requiredMonthly = "€1,200",
            requiredYearly = "€14,400",
            avgMonthlyGrowth = "+€683",
            yearsAtCurrentPace = "5Y 4M"
        )
    }
}
