package com.agcoding.networkapp.fixedexpenses.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.fixedexpenses.presentation.model.AccountExpenseStatsUiModel
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun AccountStatsCard(
    stats: List<AccountExpenseStatsUiModel>,
    modifier: Modifier = Modifier,
) {
    if (stats.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = "BY ACCOUNT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(12.dp))

            stats.forEachIndexed { index, stat ->
                AccountStatsRow(stat = stat)
                if (index < stats.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountStatsRow(
    stat: AccountExpenseStatsUiModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = runCatching { Color(android.graphics.Color.parseColor(stat.accountColorHex)) }
                            .getOrDefault(MaterialTheme.colorScheme.primary),
                        shape = CircleShape,
                    )
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = stat.accountName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${stat.count} expense${if (stat.count != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "${stat.formattedMonthlyTotal} / mo",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountStatsCardPreview() {
    NetWorthTheme {
        AccountStatsCard(
            stats = listOf(
                AccountExpenseStatsUiModel(1, "Main", "#76C893", 3, "€1,265.99"),
                AccountExpenseStatsUiModel(2, "Savings", "#5B8DEF", 1, "€50.00"),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
