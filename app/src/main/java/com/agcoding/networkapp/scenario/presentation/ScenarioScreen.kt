package com.agcoding.networkapp.scenario.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.scenario.domain.model.Affordability
import com.agcoding.networkapp.scenario.domain.model.HomeExpenseType
import com.agcoding.networkapp.scenario.presentation.components.CountStepper
import com.agcoding.networkapp.scenario.presentation.components.MoneyField
import com.agcoding.networkapp.scenario.presentation.components.ScenarioBottomBar
import com.agcoding.networkapp.scenario.presentation.components.ScenarioResultCard
import com.agcoding.networkapp.scenario.presentation.components.ScenarioSectionCard
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun ScenarioScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSavingsPlanner: () -> Unit,
    viewModel: ScenarioViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScenarioContent(
        uiState = uiState,
        onIntent = { intent ->
            if (intent == ScenarioIntent.NavigateToSavingsPlanner) onNavigateToSavingsPlanner()
            else viewModel.onIntent(intent)
        },
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenarioContent(
    uiState: ScenarioUiState,
    onIntent: (ScenarioIntent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scenario_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(ScenarioIntent.Reset) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.scenario_reset))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            uiState.result?.let { result ->
                if (uiState.moveEnabled || uiState.carsEnabled) ScenarioBottomBar(result)
            }
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.scenario_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            item { MoveSection(uiState, onIntent) }
            item { CarsSection(uiState, onIntent) }

            if (uiState.payers.size > 1 && (uiState.moveEnabled || uiState.carsEnabled)) {
                item { PayerSelector(uiState, onIntent) }
            }

            uiState.result?.let { result ->
                item {
                    ScenarioResultCard(
                        result = result,
                        onAddSalaries = { onIntent(ScenarioIntent.NavigateToSavingsPlanner) },
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.scenario_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MoveSection(uiState: ScenarioUiState, onIntent: (ScenarioIntent) -> Unit) {
    ScenarioSectionCard(
        emoji = "🏠",
        title = stringResource(R.string.scenario_move_title),
        subtitle = stringResource(R.string.scenario_move_subtitle),
        checked = uiState.moveEnabled,
        onCheckedChange = { onIntent(ScenarioIntent.ToggleMove(it)) },
    ) {
        uiState.homeCosts.forEach { cost ->
            MoneyField(
                value = cost.input,
                onValueChange = { onIntent(ScenarioIntent.UpdateHomeCost(cost.type, it)) },
                label = "${cost.type.emoji} ${stringResource(cost.type.labelRes)}",
                currencySymbol = uiState.currencySymbol,
                supportingText = cost.formattedCurrent?.let { stringResource(R.string.scenario_now_per_month, it) }
                    ?: stringResource(R.string.scenario_not_today),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        MoneyField(
            value = uiState.moveOneOffInput,
            onValueChange = { onIntent(ScenarioIntent.UpdateMoveOneOff(it)) },
            label = stringResource(R.string.scenario_move_one_off),
            currencySymbol = uiState.currencySymbol,
            supportingText = stringResource(R.string.scenario_move_one_off_hint),
        )
    }
}

@Composable
private fun CarsSection(uiState: ScenarioUiState, onIntent: (ScenarioIntent) -> Unit) {
    ScenarioSectionCard(
        emoji = "🚗",
        title = stringResource(R.string.scenario_cars_title),
        subtitle = stringResource(R.string.scenario_cars_subtitle),
        checked = uiState.carsEnabled,
        onCheckedChange = { onIntent(ScenarioIntent.ToggleCars(it)) },
    ) {
        if (uiState.carLines.isNotEmpty()) {
            CountStepper(
                label = stringResource(R.string.scenario_cars_current),
                value = uiState.currentCars,
                onValueChange = { onIntent(ScenarioIntent.SetCurrentCars(it)) },
                min = 0,
                max = ScenarioUiState.MAX_CARS,
            )
        }
        CountStepper(
            label = stringResource(R.string.scenario_cars_new),
            value = uiState.newCars,
            onValueChange = { onIntent(ScenarioIntent.SetNewCars(it)) },
            min = 0,
            max = ScenarioUiState.MAX_CARS,
        )

        if (uiState.needsNewCarEstimate) {
            MoneyField(
                value = uiState.newCarMonthlyInput,
                onValueChange = { onIntent(ScenarioIntent.UpdateNewCarMonthly(it)) },
                label = stringResource(R.string.scenario_cars_per_new_car),
                currencySymbol = uiState.currencySymbol,
                supportingText = stringResource(R.string.scenario_cars_per_new_car_hint),
            )
        } else {
            Text(
                text = stringResource(R.string.scenario_cars_increase),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScenarioUiState.INCREASE_OPTIONS.forEach { percent ->
                    FilterChip(
                        selected = uiState.increasePercent == percent,
                        onClick = { onIntent(ScenarioIntent.SetIncreasePercent(percent)) },
                        label = { Text("+$percent%") },
                        colors = chipColors(),
                    )
                }
            }
            Text(
                text = stringResource(R.string.scenario_cars_lines_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.carLines.forEach { line ->
                MoneyField(
                    value = line.input,
                    onValueChange = { onIntent(ScenarioIntent.UpdateCarLine(line.expenseId, it)) },
                    label = line.title,
                    currencySymbol = uiState.currencySymbol,
                    supportingText = stringResource(R.string.scenario_now_per_month, line.formattedCurrent),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        MoneyField(
            value = uiState.carPaymentInput,
            onValueChange = { onIntent(ScenarioIntent.UpdateCarPayment(it)) },
            label = stringResource(R.string.scenario_cars_payment),
            currencySymbol = uiState.currencySymbol,
            supportingText = stringResource(R.string.scenario_cars_payment_hint),
        )
        MoneyField(
            value = uiState.carOneOffInput,
            onValueChange = { onIntent(ScenarioIntent.UpdateCarOneOff(it)) },
            label = stringResource(R.string.scenario_cars_one_off),
            currencySymbol = uiState.currencySymbol,
            supportingText = stringResource(R.string.scenario_cars_one_off_hint),
        )
    }
}

@Composable
private fun PayerSelector(uiState: ScenarioUiState, onIntent: (ScenarioIntent) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = stringResource(R.string.scenario_payer).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = uiState.payerAccountId == null,
                onClick = { onIntent(ScenarioIntent.SelectPayer(null)) },
                label = { Text(stringResource(R.string.scenario_payer_shared)) },
                colors = chipColors(),
            )
            uiState.payers.forEach { payer ->
                FilterChip(
                    selected = uiState.payerAccountId == payer.id,
                    onClick = { onIntent(ScenarioIntent.SelectPayer(payer.id)) },
                    label = { Text(payer.name) },
                    colors = chipColors(),
                )
            }
        }
        Text(
            text = stringResource(R.string.scenario_payer_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
    selectedLabelColor = MaterialTheme.colorScheme.surface,
)

private val HomeExpenseType.emoji: String
    get() = when (this) {
        HomeExpenseType.RENT           -> "🔑"
        HomeExpenseType.ELECTRICITY    -> "⚡"
        HomeExpenseType.WATER          -> "💧"
        HomeExpenseType.COMMON_CHARGES -> "🏢"
        HomeExpenseType.INTERNET       -> "🌐"
        HomeExpenseType.HEATING        -> "🔥"
    }

private val HomeExpenseType.labelRes: Int
    get() = when (this) {
        HomeExpenseType.RENT           -> R.string.scenario_home_rent
        HomeExpenseType.ELECTRICITY    -> R.string.scenario_home_electricity
        HomeExpenseType.WATER          -> R.string.scenario_home_water
        HomeExpenseType.COMMON_CHARGES -> R.string.scenario_home_common_charges
        HomeExpenseType.INTERNET       -> R.string.scenario_home_internet
        HomeExpenseType.HEATING        -> R.string.scenario_home_heating
    }

@Preview(showBackground = true)
@Composable
private fun ScenarioContentPreview() {
    NetWorthTheme {
        ScenarioContent(
            uiState = ScenarioUiState(
                isLoading = false,
                moveEnabled = true,
                homeCosts = listOf(
                    HomeCostUiModel(HomeExpenseType.RENT, "700", "€350.00"),
                    HomeCostUiModel(HomeExpenseType.ELECTRICITY, "90", "€75.00"),
                    HomeCostUiModel(HomeExpenseType.COMMON_CHARGES, "60", null),
                ),
                payers = listOf(PayerUiModel(1, "Anastasis"), PayerUiModel(2, "Xristina")),
                result = ScenarioResultUiModel(
                    hasSalary = true,
                    formattedExpensesBefore = "€736.67",
                    formattedExpensesAfter = "€1,161.67",
                    formattedExpenseChange = "+€425.00",
                    expensesIncrease = true,
                    formattedSavingsBefore = "€2,763.33",
                    formattedSavingsAfter = "€2,338.33",
                    isDeficitAfter = false,
                    savingsRateBefore = 79,
                    savingsRateAfter = 67,
                    affordability = Affordability.COMFORTABLE,
                    formattedFiveYearDifference = "-€25,500",
                    formattedOneOff = "€2,100",
                    monthsToCoverOneOff = 1,
                    perAccount = emptyList(),
                ),
            ),
            onIntent = {},
            onNavigateBack = {},
        )
    }
}
