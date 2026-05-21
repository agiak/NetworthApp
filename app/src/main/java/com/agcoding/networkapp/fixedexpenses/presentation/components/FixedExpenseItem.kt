package com.agcoding.networkapp.fixedexpenses.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.presentation.model.FixedExpenseUiModel
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

private val DOT_SIZE = 10.dp
private val DOT_OVERLAP = 6.dp
private const val MAX_VISIBLE_DOTS = 3

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
                if (expense.accountColors.isNotEmpty()) {
                    AccountDots(
                        hexColors = expense.accountColors,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountDots(
    hexColors: List<String>,
    modifier: Modifier = Modifier,
) {
    val visible = hexColors.take(MAX_VISIBLE_DOTS)
    val overflow = hexColors.size - MAX_VISIBLE_DOTS

    // Each dot is offset left by (DOT_OVERLAP * index) so they overlap
    val totalWidth = DOT_SIZE + DOT_OVERLAP * (visible.size - 1) +
            if (overflow > 0) DOT_OVERLAP else 0.dp

    Box(modifier = modifier.width(totalWidth)) {
        visible.forEachIndexed { index, hex ->
            val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                .getOrDefault(MaterialTheme.colorScheme.primary)
            Box(
                modifier = Modifier
                    .offset(x = DOT_OVERLAP * index)
                    .size(DOT_SIZE)
                    .background(
                        color = color,
                        shape = CircleShape,
                    )
            )
        }
        // Overflow dot: "+N"
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .offset(x = DOT_OVERLAP * visible.size)
                    .size(DOT_SIZE)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$overflow",
                    fontSize = 5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FixedExpenseItemPreview() {
    NetWorthTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Single account
            FixedExpenseItem(
                expense = FixedExpenseUiModel(
                    id = 1, title = "Rent", note = "Apartment",
                    formattedCost = "€950.00 / mo", costRaw = 950.0,
                    formattedDate = null, recurrence = RecurrenceType.MONTHLY,
                    monthlyEquivalent = null,
                    accountColors = listOf("#76C893"),
                ),
                onClick = {},
            )
            // Two accounts
            FixedExpenseItem(
                expense = FixedExpenseUiModel(
                    id = 2, title = "Netflix", note = "Family plan",
                    formattedCost = "€17.99 / mo", costRaw = 17.99,
                    formattedDate = null, recurrence = RecurrenceType.MONTHLY,
                    monthlyEquivalent = null,
                    accountColors = listOf("#76C893", "#5B8DEF"),
                ),
                onClick = {},
            )
            // Three accounts
            FixedExpenseItem(
                expense = FixedExpenseUiModel(
                    id = 3, title = "YouTube Premium", note = "Family",
                    formattedCost = "€13.99 / mo", costRaw = 13.99,
                    formattedDate = null, recurrence = RecurrenceType.MONTHLY,
                    monthlyEquivalent = null,
                    accountColors = listOf("#76C893", "#5B8DEF", "#A78BFA"),
                ),
                onClick = {},
            )
            // All accounts (no dots)
            FixedExpenseItem(
                expense = FixedExpenseUiModel(
                    id = 4, title = "iCloud Storage", note = "",
                    formattedCost = "€2.99 / mo", costRaw = 2.99,
                    formattedDate = null, recurrence = RecurrenceType.MONTHLY,
                    monthlyEquivalent = null,
                    accountColors = emptyList(),
                ),
                onClick = {},
            )
            // Annual with monthly equivalent
            FixedExpenseItem(
                expense = FixedExpenseUiModel(
                    id = 5, title = "Car Insurance", note = "Full cover",
                    formattedCost = "€840.00 / yr", costRaw = 840.0,
                    formattedDate = "1 Mar 2026", recurrence = RecurrenceType.ANNUAL,
                    monthlyEquivalent = "€70.00 / mo",
                    accountColors = listOf("#76C893"),
                ),
                onClick = {},
            )
        }
    }
}
