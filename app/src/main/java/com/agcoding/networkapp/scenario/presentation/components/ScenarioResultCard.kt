package com.agcoding.networkapp.scenario.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.scenario.domain.model.Affordability
import com.agcoding.networkapp.scenario.presentation.ScenarioResultUiModel
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

private val TightAmber = Color(0xFFF59E0B)

@Composable
private fun Affordability.color(): Color = when (this) {
    Affordability.COMFORTABLE    -> PositiveGreen
    Affordability.TIGHT          -> TightAmber
    Affordability.NOT_AFFORDABLE -> LocalAppColorScheme.current.statusError
    Affordability.UNKNOWN        -> MaterialTheme.colorScheme.onSurfaceVariant
}

private val Affordability.emoji: String
    get() = when (this) {
        Affordability.COMFORTABLE    -> "✅"
        Affordability.TIGHT          -> "⚠️"
        Affordability.NOT_AFFORDABLE -> "⛔"
        Affordability.UNKNOWN        -> "ℹ️"
    }

@Composable
private fun Affordability.title(): String = stringResource(
    when (this) {
        Affordability.COMFORTABLE    -> R.string.scenario_verdict_comfortable
        Affordability.TIGHT          -> R.string.scenario_verdict_tight
        Affordability.NOT_AFFORDABLE -> R.string.scenario_verdict_not_affordable
        Affordability.UNKNOWN        -> R.string.scenario_verdict_unknown
    }
)

/** Today vs the scenario: expenses, savings, verdict, one-off costs and per person. */
@Composable
fun ScenarioResultCard(
    result: ScenarioResultUiModel,
    onAddSalaries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = result.affordability.color()
    val errorColor = LocalAppColorScheme.current.statusError
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = 0.12f)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = result.affordability.emoji, fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = result.affordability.title(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                        )
                        if (result.affordability == Affordability.TIGHT || result.affordability == Affordability.COMFORTABLE) {
                            Text(
                                text = stringResource(R.string.scenario_verdict_rate, result.savingsRateAfter ?: 0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            ComparisonHeader()
            ComparisonRow(
                label = stringResource(R.string.scenario_fixed_expenses),
                before = result.formattedExpensesBefore,
                after = result.formattedExpensesAfter,
            )
            Text(
                text = stringResource(R.string.scenario_change_per_month, result.formattedExpenseChange),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (result.expensesIncrease) errorColor else PositiveGreen,
            )

            if (result.hasSalary) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ComparisonRow(
                    label = stringResource(R.string.scenario_savings),
                    before = result.formattedSavingsBefore,
                    after = result.formattedSavingsAfter,
                    afterColor = if (result.isDeficitAfter) errorColor else PositiveGreen,
                    bold = true,
                )
                if (result.savingsRateBefore != null && result.savingsRateAfter != null) {
                    ComparisonRow(
                        label = stringResource(R.string.scenario_savings_rate),
                        before = "${result.savingsRateBefore}%",
                        after = "${result.savingsRateAfter}%",
                    )
                }
                result.formattedFiveYearDifference?.let {
                    Text(
                        text = stringResource(R.string.scenario_five_years, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.scenario_no_salary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onAddSalaries) {
                    Text(stringResource(R.string.savings_calc_edit_salaries), color = PositiveGreen, fontWeight = FontWeight.SemiBold)
                }
            }

            result.formattedOneOff?.let { oneOff ->
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.scenario_one_off_total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = oneOff, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = result.monthsToCoverOneOff?.let {
                        pluralStringResource(R.plurals.scenario_one_off_months, it, it)
                    } ?: stringResource(R.string.scenario_one_off_never),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (result.perAccount.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Text(
                    text = stringResource(R.string.savings_planner_per_person).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                result.perAccount.forEach { account ->
                    ComparisonRow(
                        label = account.name,
                        before = account.formattedSavingsBefore,
                        after = account.formattedSavingsAfter,
                        afterColor = if (account.isDeficitAfter) errorColor else MaterialTheme.colorScheme.onSurface,
                        dotColor = parseColor(account.colorHex),
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonHeader() {
    Row {
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.scenario_now),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = stringResource(R.string.scenario_after),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.width(96.dp),
        )
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    before: String,
    after: String,
    afterColor: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false,
    dotColor: Color? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (dotColor != null) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = before,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = after,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = afterColor,
            modifier = Modifier.width(96.dp),
        )
    }
}

/** Compact "savings now → after" bar that stays visible while editing. */
@Composable
fun ScenarioBottomBar(result: ScenarioResultUiModel) {
    val accent = result.affordability.color()
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = result.affordability.emoji, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (result.hasSalary) R.string.scenario_savings else R.string.scenario_fixed_expenses),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                )
                Text(
                    text = if (result.hasSalary) "${result.formattedSavingsBefore} → ${result.formattedSavingsAfter}"
                           else "${result.formattedExpensesBefore} → ${result.formattedExpensesAfter}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = result.affordability.title(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
    }
}

private fun parseColor(hex: String): Color =
    try { Color(android.graphics.Color.parseColor(hex)) } catch (e: IllegalArgumentException) { Color.Gray }
