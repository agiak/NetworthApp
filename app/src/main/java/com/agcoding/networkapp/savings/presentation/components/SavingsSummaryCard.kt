package com.agcoding.networkapp.savings.presentation.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.savings.presentation.model.SavingsSummaryUiModel
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun SavingsSummaryCard(
    summary: SavingsSummaryUiModel,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColorScheme.current
    val savingsColor = if (summary.isDeficit) colors.statusError else PositiveGreen

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.backgroundCard,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text(
                text = stringResource(R.string.savings_planner_should_save_together).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${summary.formattedSavings} ${stringResource(R.string.savings_planner_per_month_suffix)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = savingsColor,
            )
            Text(
                text = stringResource(R.string.savings_planner_per_year, summary.formattedYearlySavings),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (summary.hasSalary) {
                Spacer(Modifier.height(14.dp))
                SavingsProgressBar(savingsShare = summary.savingsShare, isDeficit = summary.isDeficit)
                Spacer(Modifier.height(6.dp))
                SavingsRateLabel(ratePercent = summary.savingsRatePercent, isDeficit = summary.isDeficit)
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryValue(
                    label = stringResource(R.string.savings_planner_total_salary),
                    value = summary.formattedSalary,
                )
                SummaryValue(
                    label = stringResource(R.string.savings_planner_fixed_expenses),
                    value = "−${summary.formattedFixedExpenses}",
                    alignEnd = true,
                )
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, alignEnd: Boolean = false) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SavingsSummaryCardPreview() {
    NetWorthTheme {
        SavingsSummaryCard(
            summary = SavingsSummaryUiModel(
                formattedSalary        = "€3,500.00",
                formattedFixedExpenses = "€840.00",
                formattedSavings       = "€2,660.00",
                formattedYearlySavings = "€31,920.00",
                savingsRatePercent     = 76,
                savingsShare           = 0.76f,
                isDeficit              = false,
                hasSalary              = true,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
