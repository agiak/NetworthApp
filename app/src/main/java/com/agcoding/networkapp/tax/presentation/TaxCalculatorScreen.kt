package com.agcoding.networkapp.tax.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen
import com.agcoding.networkapp.shared.ui.utils.ThousandSeparatorTransformation

@Composable
fun TaxCalculatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: TaxCalculatorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TaxCalculatorContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaxCalculatorContent(
    uiState: TaxCalculatorUiState,
    onIntent: (TaxCalculatorIntent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tax_calc_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { InputsCard(uiState = uiState, onIntent = onIntent) }

            val result = uiState.result
            if (result == null) {
                item {
                    Text(
                        text = stringResource(R.string.tax_calc_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                    )
                }
            } else {
                item { TaxDueCard(result = result) }
                item { BreakdownCard(result = result, ratePercent = uiState.ratePercent) }
                item {
                    Text(
                        text = stringResource(R.string.tax_calc_note, uiState.ratePercent),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun InputsCard(
    uiState: TaxCalculatorUiState,
    onIntent: (TaxCalculatorIntent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AmountField(
                value = uiState.turnoverInput,
                onValueChange = { onIntent(TaxCalculatorIntent.UpdateTurnover(it)) },
                label = stringResource(R.string.tax_calc_turnover),
                prefix = uiState.currencySymbol,
            )
            AmountField(
                value = uiState.expensesInput,
                onValueChange = { onIntent(TaxCalculatorIntent.UpdateExpenses(it)) },
                label = stringResource(R.string.tax_calc_expenses),
                prefix = uiState.currencySymbol,
            )
        }
    }
}

/** The headline: how much tax, or that a loss pays none. */
@Composable
private fun TaxDueCard(result: TaxResultUiModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PositiveGreen.copy(alpha = 0.12f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.tax_calc_tax_due),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = result.formattedTax,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = PositiveGreen,
            )
            Text(
                text = if (result.isLoss) stringResource(R.string.tax_calc_loss, result.formattedProfit)
                       else stringResource(R.string.tax_calc_monthly_reserve, result.formattedMonthlyReserve),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Turnover − expenses = profit, − tax = profit after tax. */
@Composable
private fun BreakdownCard(result: TaxResultUiModel, ratePercent: Int) {
    val profitColor = if (result.isLoss) LocalAppColorScheme.current.statusError else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BreakdownLine(stringResource(R.string.tax_calc_turnover), result.formattedTurnover)
            BreakdownLine(stringResource(R.string.tax_calc_expenses), "−${result.formattedExpenses}")
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            BreakdownLine(stringResource(R.string.tax_calc_profit), result.formattedProfit, valueColor = profitColor, bold = true)
            BreakdownLine(stringResource(R.string.tax_calc_tax_line, ratePercent), "−${result.formattedTax}")
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            BreakdownLine(
                label = stringResource(R.string.tax_calc_profit_after_tax),
                value = result.formattedProfitAfterTax,
                valueColor = if (result.isLoss) profitColor else PositiveGreen,
                bold = true,
            )
        }
    }
}

@Composable
private fun BreakdownLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor,
        )
    }
}

@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    prefix: String,
) {
    val textStyle = MaterialTheme.typography.titleLarge
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Text(text = prefix, style = textStyle, fontWeight = FontWeight.Bold, color = PositiveGreen)
        },
        singleLine = true,
        visualTransformation = ThousandSeparatorTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PositiveGreen,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            cursorColor = PositiveGreen,
        ),
        textStyle = textStyle.copy(fontWeight = FontWeight.Bold),
    )
}

@Preview(showBackground = true)
@Composable
private fun TaxCalculatorContentPreview() {
    NetWorthTheme {
        TaxCalculatorContent(
            uiState = TaxCalculatorUiState(
                turnoverInput = "80000",
                expensesInput = "30000",
                result = TaxResultUiModel(
                    formattedTurnover       = "€80,000.00",
                    formattedExpenses       = "€30,000.00",
                    formattedProfit         = "€50,000.00",
                    formattedTax            = "€11,000.00",
                    formattedProfitAfterTax = "€39,000.00",
                    formattedMonthlyReserve = "€916.67",
                    isLoss                  = false,
                ),
            ),
            onIntent = {},
            onNavigateBack = {},
        )
    }
}
