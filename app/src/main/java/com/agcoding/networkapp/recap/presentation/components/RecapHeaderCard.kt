package com.agcoding.networkapp.recap.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.agcoding.networkapp.shared.ui.theme.DarkBackground
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun RecapHeaderCard(
    year: Int,
    totalGrowthFormatted: String,
    totalGrowthPercent: String,
    totalGrowthPositive: Boolean,
    startValue: String,
    endValue: String,
    monthsTracked: Int,
    isNewAllTimeHigh: Boolean,
    modifier: Modifier = Modifier
) {
    val growthColor = if (totalGrowthPositive) PositiveGreen else Color(0xFFEF5350)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBackground)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.displayMedium.copy(letterSpacing = 4.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.recap_title),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.recap_year_growth_title).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = totalGrowthFormatted,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = growthColor
                )
                Text(
                    text = "  $totalGrowthPercent",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = growthColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.recap_from_to, startValue, endValue),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.recap_months_tracked, monthsTracked),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.35f)
                )
                if (isNewAllTimeHigh) {
                    Text(
                        text = "★ " + stringResource(R.string.recap_new_all_time_high),
                        style = MaterialTheme.typography.labelSmall,
                        color = PositiveGreen.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecapHeaderCardPreview() {
    NetWorthTheme {
        RecapHeaderCard(
            year = 2025,
            totalGrowthFormatted = "+€18,400",
            totalGrowthPercent = "+24.5%",
            totalGrowthPositive = true,
            startValue = "€58,000",
            endValue = "€76,400",
            monthsTracked = 12,
            isNewAllTimeHigh = true
        )
    }
}
