package com.agcoding.networkapp.compare.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.agcoding.networkapp.compare.presentation.components.CompareDualChart
import com.agcoding.networkapp.compare.presentation.components.CompareInsightCard
import com.agcoding.networkapp.compare.presentation.components.CompareStatCard
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ActivePicker {
    CURRENT_START, CURRENT_END, PREVIOUS_START, PREVIOUS_END
}

@Composable
fun CompareScreen(
    onNavigateBack: () -> Unit,
    viewModel: CompareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CompareContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompareContent(
    uiState: CompareUiState,
    onIntent: (CompareIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var activePicker by remember { mutableStateOf<ActivePicker?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(CompareIntent.ClearError)
        }
    }

    // Date picker dialog
    activePicker?.let { picker ->
        val initialMillis = when (picker) {
            ActivePicker.CURRENT_START -> uiState.customCurrentStart
            ActivePicker.CURRENT_END -> uiState.customCurrentEnd
            ActivePicker.PREVIOUS_START -> uiState.customPreviousStart
            ActivePicker.PREVIOUS_END -> uiState.customPreviousEnd
        }
        val dpState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { activePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { millis ->
                        when (picker) {
                            ActivePicker.CURRENT_START -> onIntent(CompareIntent.SetCustomCurrentStart(millis))
                            ActivePicker.CURRENT_END -> onIntent(CompareIntent.SetCustomCurrentEnd(millis))
                            ActivePicker.PREVIOUS_START -> onIntent(CompareIntent.SetCustomPreviousStart(millis))
                            ActivePicker.PREVIOUS_END -> onIntent(CompareIntent.SetCustomPreviousEnd(millis))
                        }
                    }
                    activePicker = null
                }) { Text(stringResource(R.string.btn_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { activePicker = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = dpState)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.compare_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ModeSelector(
                            selectedMode = uiState.selectedMode,
                            onModeSelect = { onIntent(CompareIntent.SelectMode(it)) }
                        )
                    }

                    item {
                        AnimatedVisibility(
                            visible = uiState.selectedMode == CompareMode.CUSTOM,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            CustomPeriodSelector(
                                currentStart = uiState.customCurrentStart,
                                currentEnd = uiState.customCurrentEnd,
                                previousStart = uiState.customPreviousStart,
                                previousEnd = uiState.customPreviousEnd,
                                onPickCurrentStart = { activePicker = ActivePicker.CURRENT_START },
                                onPickCurrentEnd = { activePicker = ActivePicker.CURRENT_END },
                                onPickPreviousStart = { activePicker = ActivePicker.PREVIOUS_START },
                                onPickPreviousEnd = { activePicker = ActivePicker.PREVIOUS_END }
                            )
                        }
                    }

                    item {
                        AnimatedContent(
                            targetState = uiState.selectedMode to uiState.hasData,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "compareContent"
                        ) { (_, hasData) ->
                            if (!hasData) {
                                val emptyText = if (uiState.selectedMode == CompareMode.CUSTOM)
                                    stringResource(R.string.compare_custom_incomplete)
                                else
                                    stringResource(R.string.compare_no_data)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = emptyText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                CompareBody(uiState = uiState)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CompareBody(uiState: CompareUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.currentChartData.size >= 2 && uiState.previousChartData.size >= 2) {
            CompareDualChart(
                currentData = uiState.currentChartData,
                previousData = uiState.previousChartData,
                currentLabel = uiState.currentLabel,
                previousLabel = uiState.previousLabel
            )
        }

        CompareStatCard(
            label = stringResource(R.string.compare_total_growth),
            previousLabel = uiState.previousLabel,
            previousValue = uiState.previousTotalGrowth,
            previousPositive = uiState.previousTotalGrowthPositive,
            currentLabel = uiState.currentLabel,
            currentValue = uiState.currentTotalGrowth,
            currentPositive = uiState.currentTotalGrowthPositive,
            diff = uiState.growthDiff,
            improved = uiState.growthImproved
        )

        CompareStatCard(
            label = stringResource(R.string.compare_avg_monthly),
            previousLabel = uiState.previousLabel,
            previousValue = uiState.previousAvgMonthly,
            previousPositive = true,
            currentLabel = uiState.currentLabel,
            currentValue = uiState.currentAvgMonthly,
            currentPositive = true,
            diff = if (uiState.avgMonthlyChangePercent.isNotEmpty())
                "${uiState.avgMonthlyDiff} (${uiState.avgMonthlyChangePercent})"
            else uiState.avgMonthlyDiff,
            improved = uiState.avgMonthlyImproved
        )

        val (speedEmoji, speedTitle, speedDesc, speedColor) = when (uiState.speed) {
            CompareSpeed.ACCELERATING -> SpeedConfig(
                "🚀", stringResource(R.string.compare_speed_accelerating),
                stringResource(R.string.compare_speed_accelerating_desc), PositiveGreen
            )
            CompareSpeed.SLOWING -> SpeedConfig(
                "📉", stringResource(R.string.compare_speed_slowing),
                stringResource(R.string.compare_speed_slowing_desc), MaterialTheme.colorScheme.error
            )
            CompareSpeed.STABLE -> SpeedConfig(
                "📊", stringResource(R.string.compare_speed_stable),
                stringResource(R.string.compare_speed_stable_desc), MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        CompareInsightCard(
            sectionLabel = stringResource(R.string.compare_growth_speed),
            emoji = speedEmoji, title = speedTitle, description = speedDesc, titleColor = speedColor
        )

        val (stabEmoji, stabTitle, stabDesc) = when (uiState.stability) {
            CompareStability.MORE_STABLE -> StabilityConfig(
                "✅", stringResource(R.string.compare_stability_better),
                stringResource(R.string.compare_stability_better_desc)
            )
            CompareStability.LESS_STABLE -> StabilityConfig(
                "⚠️", stringResource(R.string.compare_stability_worse),
                stringResource(R.string.compare_stability_worse_desc)
            )
            CompareStability.SIMILAR -> StabilityConfig(
                "〰️", stringResource(R.string.compare_stability_similar),
                stringResource(R.string.compare_stability_similar_desc)
            )
        }
        CompareInsightCard(
            sectionLabel = stringResource(R.string.compare_stability),
            emoji = stabEmoji, title = stabTitle, description = stabDesc,
            titleColor = when (uiState.stability) {
                CompareStability.MORE_STABLE -> PositiveGreen
                CompareStability.LESS_STABLE -> MaterialTheme.colorScheme.error
                CompareStability.SIMILAR -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            subInfo = if (uiState.stabilityCurrentVolatility.isNotEmpty() && uiState.stabilityPreviousVolatility.isNotEmpty())
                "±${uiState.stabilityCurrentVolatility} vs ±${uiState.stabilityPreviousVolatility} before"
            else ""
        )

        if (uiState.bestMonthLabel.isNotEmpty() && uiState.bestMonthValue.isNotEmpty()) {
            CompareInsightCard(
                sectionLabel = stringResource(R.string.compare_best_period),
                emoji = "⭐",
                title = uiState.bestMonthLabel,
                description = stringResource(R.string.compare_best_month_desc, uiState.bestMonthLabel, uiState.bestMonthValue),
                titleColor = PositiveGreen,
                subInfo = uiState.bestMonthValue
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomPeriodSelector(
    currentStart: Long?,
    currentEnd: Long?,
    previousStart: Long?,
    previousEnd: Long?,
    onPickCurrentStart: () -> Unit,
    onPickCurrentEnd: () -> Unit,
    onPickPreviousStart: () -> Unit,
    onPickPreviousEnd: () -> Unit
) {
    val displayFmt = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }
    val notSet = stringResource(R.string.compare_custom_not_set)

    fun fmtMs(millis: Long?): String = millis?.let {
        val ym = YearMonth.from(LocalDate.ofEpochDay(it / 86_400_000L))
        ym.format(displayFmt)
    } ?: notSet

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PeriodPickerSection(
                sectionLabel = stringResource(R.string.compare_custom_period_b),
                fromLabel = stringResource(R.string.compare_custom_from),
                toLabel = stringResource(R.string.compare_custom_to),
                fromValue = fmtMs(previousStart),
                toValue = fmtMs(previousEnd),
                fromIsSet = previousStart != null,
                toIsSet = previousEnd != null,
                onPickFrom = onPickPreviousStart,
                onPickTo = onPickPreviousEnd
            )
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            PeriodPickerSection(
                sectionLabel = stringResource(R.string.compare_custom_period_a),
                fromLabel = stringResource(R.string.compare_custom_from),
                toLabel = stringResource(R.string.compare_custom_to),
                fromValue = fmtMs(currentStart),
                toValue = fmtMs(currentEnd),
                fromIsSet = currentStart != null,
                toIsSet = currentEnd != null,
                onPickFrom = onPickCurrentStart,
                onPickTo = onPickCurrentEnd
            )
        }
    }
}

@Composable
private fun PeriodPickerSection(
    sectionLabel: String,
    fromLabel: String,
    toLabel: String,
    fromValue: String,
    toValue: String,
    fromIsSet: Boolean,
    toIsSet: Boolean,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit
) {
    Text(
        text = sectionLabel.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DatePickerChip(
            label = fromLabel,
            value = fromValue,
            isSet = fromIsSet,
            onClick = onPickFrom,
            modifier = Modifier.weight(1f)
        )
        DatePickerChip(
            label = toLabel,
            value = toValue,
            isSet = toIsSet,
            onClick = onPickTo,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DatePickerChip(
    label: String,
    value: String,
    isSet: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSet) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSet) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = if (isSet) PositiveGreen.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

@Composable
private fun ModeSelector(selectedMode: CompareMode, onModeSelect: (CompareMode) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        val modes = listOf(
            CompareMode.YEAR_VS_YEAR to R.string.compare_mode_year,
            CompareMode.HALF_VS_HALF to R.string.compare_mode_half,
            CompareMode.QUARTER_VS_QUARTER to R.string.compare_mode_quarter,
            CompareMode.YEAR_ROLLING to R.string.compare_mode_rolling_year,
            CompareMode.CUSTOM to R.string.compare_mode_custom,
        )
        items(modes.size) { i ->
            val (mode, labelRes) = modes[i]
            ModeChip(
                label = stringResource(labelRes),
                selected = selectedMode == mode,
                onClick = { onModeSelect(mode) }
            )
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = MaterialTheme.colorScheme.surface,
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            selectedBorderColor = Color.Transparent
        )
    )
}

private data class SpeedConfig(val emoji: String, val title: String, val desc: String, val color: Color)
private data class StabilityConfig(val emoji: String, val title: String, val desc: String)

@Preview(showBackground = true)
@Composable
private fun CompareContentPreview() {
    val current = listOf(0.1f, 0.3f, 0.5f, 0.65f, 0.8f, 0.9f)
        .mapIndexed { i, y -> ChartPoint(i.toFloat() / 5f, y) }
    val previous = listOf(0.05f, 0.2f, 0.35f, 0.45f, 0.6f, 0.75f)
        .mapIndexed { i, y -> ChartPoint(i.toFloat() / 5f, y) }
    NetWorthTheme {
        CompareContent(
            uiState = CompareUiState(
                isLoading = false, hasData = true,
                selectedMode = CompareMode.YEAR_VS_YEAR,
                currentLabel = "2026", previousLabel = "2025",
                currentTotalGrowth = "+€18,400", previousTotalGrowth = "+€12,000",
                currentTotalGrowthPositive = true, previousTotalGrowthPositive = true,
                growthDiff = "€6,400", growthImproved = true,
                currentAvgMonthly = "+€1,800", previousAvgMonthly = "+€1,200",
                avgMonthlyDiff = "€600", avgMonthlyImproved = true, avgMonthlyChangePercent = "+50%",
                speed = CompareSpeed.ACCELERATING, stability = CompareStability.MORE_STABLE,
                stabilityCurrentVolatility = "€450", stabilityPreviousVolatility = "€820",
                bestMonthLabel = "March", bestMonthValue = "+€3,200",
                currentChartData = current, previousChartData = previous
            ),
            onIntent = {}, onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomSelectorPreview() {
    NetWorthTheme {
        CustomPeriodSelector(
            currentStart = null, currentEnd = null,
            previousStart = null, previousEnd = null,
            onPickCurrentStart = {}, onPickCurrentEnd = {},
            onPickPreviousStart = {}, onPickPreviousEnd = {}
        )
    }
}
