package com.agcoding.networkapp.savings.presentation.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.agcoding.networkapp.savings.presentation.model.SavingsAccountUiModel
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun SavingsAccountCard(
    account: SavingsAccountUiModel,
    onEditSalary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onEditSalary,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {

            // ── Header: color dot + name + edit ──────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = runCatching {
                                Color(android.graphics.Color.parseColor(account.accountColorHex))
                            }.getOrDefault(Color.Gray),
                            shape = CircleShape,
                        )
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = account.accountName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (account.hasSalary) {
                    IconButton(onClick = onEditSalary, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.btn_edit),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!account.hasSalary) {
                NoSalaryContent(account = account, onAddSalary = onEditSalary)
            } else {
                SalaryBreakdown(account = account)
            }
        }
    }
}

@Composable
private fun NoSalaryContent(
    account: SavingsAccountUiModel,
    onAddSalary: () -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.savings_planner_no_salary_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        LineItem(
            label = stringResource(R.string.savings_planner_fixed_expenses),
            detail = stringResource(R.string.fixed_expenses_count, account.expenseCount),
            value = "−${account.formattedFixedExpenses}",
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onAddSalary,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Text(stringResource(R.string.savings_planner_add_salary), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SalaryBreakdown(account: SavingsAccountUiModel) {
    val colors = LocalAppColorScheme.current
    Column {
        // ── Salary − expenses ────────────────────────────────────────────
        LineItem(
            label = stringResource(R.string.savings_planner_net_salary),
            value = account.formattedSalary,
        )
        Spacer(Modifier.height(8.dp))
        LineItem(
            label = stringResource(R.string.savings_planner_fixed_expenses),
            detail = stringResource(R.string.fixed_expenses_count, account.expenseCount),
            value = "−${account.formattedFixedExpenses}",
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(12.dp))

        // ── Result ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.savings_planner_should_save).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.savings_planner_per_year, account.formattedYearlySavings),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${account.formattedSavings} ${stringResource(R.string.savings_planner_per_month_suffix)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (account.isDeficit) colors.statusError else PositiveGreen,
            )
        }

        Spacer(Modifier.height(12.dp))
        SavingsProgressBar(savingsShare = account.savingsShare, isDeficit = account.isDeficit)
        Spacer(Modifier.height(6.dp))
        SavingsRateLabel(ratePercent = account.savingsRatePercent, isDeficit = account.isDeficit)
    }
}

@Composable
private fun LineItem(
    label: String,
    value: String,
    detail: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SavingsAccountCardPreview() {
    NetWorthTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            SavingsAccountCard(
                account = SavingsAccountUiModel(
                    accountId              = 1,
                    accountName            = "Anastasis",
                    accountColorHex        = "#76C893",
                    salaryRaw              = 2_000.0,
                    hasSalary              = true,
                    expenseCount           = 3,
                    formattedSalary        = "€2,000.00",
                    formattedFixedExpenses = "€720.00",
                    formattedSavings       = "€1,280.00",
                    formattedYearlySavings = "€15,360.00",
                    savingsRatePercent     = 64,
                    savingsShare           = 0.64f,
                    isDeficit              = false,
                ),
                onEditSalary = {},
            )
            SavingsAccountCard(
                account = SavingsAccountUiModel(
                    accountId              = 2,
                    accountName            = "Xristina",
                    accountColorHex        = "#A78BFA",
                    salaryRaw              = 0.0,
                    hasSalary              = false,
                    expenseCount           = 2,
                    formattedSalary        = "€0.00",
                    formattedFixedExpenses = "€420.00",
                    formattedSavings       = "-€420.00",
                    formattedYearlySavings = "-€5,040.00",
                    savingsRatePercent     = null,
                    savingsShare           = 0f,
                    isDeficit              = true,
                ),
                onEditSalary = {},
            )
        }
    }
}
