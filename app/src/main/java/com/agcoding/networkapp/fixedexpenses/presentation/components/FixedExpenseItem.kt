package com.agcoding.networkapp.fixedexpenses.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.presentation.model.FixedExpenseUiModel
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun FixedExpenseItem(
    expense: FixedExpenseUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "💸", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val subtitle = listOfNotNull(expense.note.ifBlank { null }, expense.formattedDate)
                    .joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = expense.formattedCost,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (expense.monthlyEquivalent != null) {
                    Text(
                        text = expense.monthlyEquivalent,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FixedExpenseItemPreview() {
    NetWorthTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FixedExpenseItem(
                expense = FixedExpenseUiModel(
                    id = 1, title = "Rent", note = "Monthly",
                    formattedCost = "€1,200.00 / mo", costRaw = 1200.0,
                    formattedDate = null, recurrence = RecurrenceType.MONTHLY,
                    monthlyEquivalent = null,
                ),
                onClick = {},
            )
            androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
            FixedExpenseItem(
                expense = FixedExpenseUiModel(
                    id = 2, title = "Car Insurance", note = "",
                    formattedCost = "€600.00 / yr", costRaw = 600.0,
                    formattedDate = "1 Jan 2026", recurrence = RecurrenceType.ANNUAL,
                    monthlyEquivalent = "€50.00 / mo",
                ),
                onClick = {},
            )
        }
    }
}
