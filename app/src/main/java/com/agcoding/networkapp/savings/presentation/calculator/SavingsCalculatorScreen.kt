package com.agcoding.networkapp.savings.presentation.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen
import com.agcoding.networkapp.shared.ui.utils.ThousandSeparatorTransformation

@Composable
fun SavingsCalculatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavingsCalculatorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SavingsCalculatorContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsCalculatorContent(
    uiState: SavingsCalculatorUiState,
    onIntent: (SavingsCalculatorIntent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.savings_calc_title), fontWeight = FontWeight.Bold) },
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
            item {
                InputsCard(
                    uiState = uiState,
                    onIntent = onIntent,
                )
            }

            if (uiState.projections.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.savings_calc_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                    )
                }
            } else {
                uiState.customProjection?.let { custom ->
                    item { ProjectionRow(projection = custom) }
                }
                item {
                    Text(
                        text = stringResource(R.string.savings_calc_results_header).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
                items(uiState.projections, key = { it.years }) { projection ->
                    ProjectionRow(projection = projection)
                }
                item {
                    Text(
                        text = stringResource(R.string.savings_calc_note),
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
    uiState: SavingsCalculatorUiState,
    onIntent: (SavingsCalculatorIntent) -> Unit,
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
            CalculatorField(
                value = uiState.monthlyAmountInput,
                onValueChange = { onIntent(SavingsCalculatorIntent.UpdateMonthlyAmount(it)) },
                label = stringResource(R.string.savings_calc_monthly_amount),
                prefix = uiState.currencySymbol,
                keyboardType = KeyboardType.Decimal,
                visualTransformation = ThousandSeparatorTransformation(),
                large = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CalculatorField(
                    value = uiState.annualReturnInput,
                    onValueChange = { onIntent(SavingsCalculatorIntent.UpdateAnnualReturn(it)) },
                    label = stringResource(R.string.savings_calc_annual_return),
                    suffix = "%",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                CalculatorField(
                    value = uiState.customYearsInput,
                    onValueChange = { onIntent(SavingsCalculatorIntent.UpdateCustomYears(it)) },
                    label = stringResource(R.string.savings_calc_custom_years),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(R.string.savings_calc_return_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalculatorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    suffix: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    large: Boolean = false,
) {
    val textStyle = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = if (prefix != null) {
            {
                Text(
                    text = prefix,
                    style = textStyle,
                    fontWeight = FontWeight.Bold,
                    color = PositiveGreen,
                )
            }
        } else null,
        trailingIcon = if (suffix != null) {
            { Text(text = suffix, style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PositiveGreen,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            cursorColor = PositiveGreen,
        ),
        textStyle = textStyle.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun ProjectionRow(
    projection: SavingsProjectionUiModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (projection.isCustom) PositiveGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pluralStringResource(R.plurals.savings_calc_years, projection.years, projection.years),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (projection.formattedReturns != null) {
                        stringResource(R.string.savings_calc_breakdown, projection.formattedDeposits, projection.formattedReturns)
                    } else {
                        stringResource(R.string.savings_calc_deposits_only, projection.formattedDeposits)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = projection.formattedTotal,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PositiveGreen,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SavingsCalculatorContentPreview() {
    NetWorthTheme {
        SavingsCalculatorContent(
            uiState = SavingsCalculatorUiState(
                monthlyAmountInput = "1000",
                annualReturnInput = "5",
                customYearsInput = "30",
                customProjection = SavingsProjectionUiModel(30, "€832,259", "€360,000", "€472,259", isCustom = true),
                projections = listOf(
                    SavingsProjectionUiModel(1, "€12,279", "€12,000", "€279"),
                    SavingsProjectionUiModel(5, "€68,006", "€60,000", "€8,006"),
                    SavingsProjectionUiModel(10, "€155,282", "€120,000", "€35,282"),
                ),
            ),
            onIntent = {},
            onNavigateBack = {},
        )
    }
}
