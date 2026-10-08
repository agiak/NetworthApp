package com.agcoding.networkapp.expenseanalysis.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.expenseanalysis.domain.model.CategorySource
import com.agcoding.networkapp.expenseanalysis.presentation.model.AccountAmountUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.AnalyzedExpenseUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.CategoryUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.color
import com.agcoding.networkapp.expenseanalysis.presentation.model.emoji
import com.agcoding.networkapp.expenseanalysis.presentation.model.labelRes

@Composable
fun CategoryCard(
    category: CategoryUiModel,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = category.category.color
    Surface(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.18f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = category.category.emoji, fontSize = 20.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(category.category.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.expense_analysis_expense_count, category.expenses.size, category.expenses.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = category.formattedMonthly,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${category.percent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            ShareBar(fraction = category.fraction, color = accent)

            if (category.perAccount.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                PerAccountRow(category.perAccount)
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    category.expenses.forEachIndexed { index, expense ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        }
                        ExpenseRow(expense = expense, onClick = { onExpenseClick(expense.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareBar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerAccountRow(amounts: List<AccountAmountUiModel>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        amounts.forEach { amount ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(parseColor(amount.colorHex)),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${amount.accountName} ${amount.formattedMonthly}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: AnalyzedExpenseUiModel, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SourceBadge(expense)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = expense.formattedMonthly,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                expense.sharePercent?.let {
                    Text(
                        text = stringResource(R.string.expense_analysis_share, it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Where the category came from: the user, what was learned, or a guess worth checking. */
@Composable
private fun SourceBadge(expense: AnalyzedExpenseUiModel) {
    val (text, color) = when {
        expense.source == CategorySource.MANUAL  -> stringResource(R.string.expense_analysis_badge_manual) to MaterialTheme.colorScheme.onSurfaceVariant
        expense.isUncertain                      -> stringResource(R.string.expense_analysis_badge_uncertain) to Color(0xFFF59E0B)
        expense.source == CategorySource.LEARNED -> stringResource(R.string.expense_analysis_badge_learned) to expense.category.color
        else                                     -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium,
    )
}

internal fun parseColor(hex: String): Color =
    try { Color(android.graphics.Color.parseColor(hex)) } catch (e: IllegalArgumentException) { Color.Gray }
