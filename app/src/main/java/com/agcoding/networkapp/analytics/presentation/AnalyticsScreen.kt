package com.agcoding.networkapp.analytics.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.analytics.presentation.components.AccountComparisonCard
import com.agcoding.networkapp.analytics.presentation.components.AnalyticsChartCard
import com.agcoding.networkapp.analytics.presentation.components.AnalyticsSummaryCard
import com.agcoding.networkapp.analytics.presentation.components.ComparePeriodsEntryCard
import com.agcoding.networkapp.analytics.presentation.components.FuturePredictionEntryCard
import com.agcoding.networkapp.analytics.presentation.components.GoalCalculatorEntryCard
import com.agcoding.networkapp.analytics.presentation.components.MonthByMonthRow
import com.agcoding.networkapp.analytics.presentation.components.YearlyRecapEntryCard
import com.agcoding.networkapp.analytics.presentation.model.MonthlyEntryUiModel
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen
import kotlinx.coroutines.launch

@Composable
fun AnalyticsScreen(
    onNavigateToAllMonths: () -> Unit,
    onNavigateToPrediction: () -> Unit,
    onNavigateToGoal: () -> Unit,
    onNavigateToRecap: () -> Unit,
    onNavigateToCompare: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AnalyticsContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateToAllMonths = onNavigateToAllMonths,
        onNavigateToPrediction = onNavigateToPrediction,
        onNavigateToGoal = onNavigateToGoal,
        onNavigateToRecap = onNavigateToRecap,
        onNavigateToCompare = onNavigateToCompare
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onIntent: (AnalyticsIntent) -> Unit,
    onNavigateToAllMonths: () -> Unit,
    onNavigateToPrediction: () -> Unit,
    onNavigateToGoal: () -> Unit,
    onNavigateToRecap: () -> Unit,
    onNavigateToCompare: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(AnalyticsIntent.ClearError)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_analytics),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (uiState.hasData) {
                        // Filter chip "6M ▼"
                        Surface(
                            onClick = { showFilterSheet = true },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = uiState.selectedFilter.toShortLabel(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            !uiState.hasData -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.analytics_no_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Account filter chips (multi-account only)
                    if (uiState.accounts.size > 1) {
                        item { AccountFilterRow(uiState.accounts, uiState.selectedAccountId) { onIntent(AnalyticsIntent.SelectAccount(it)) } }
                    }

                    // Hero chart card
                    item {
                        AnalyticsChartCard(
                            chartData           = uiState.chartData,
                            startLabel          = uiState.chartStartLabel,
                            midLabel            = uiState.chartMidLabel,
                            endLabel            = uiState.chartEndLabel,
                            headerLabel         = uiState.filterPeriodLabel,
                            currentNetWorth     = uiState.currentNetWorthFormatted,
                            totalGrowth         = uiState.totalGrowth,
                            totalGrowthPercent  = uiState.totalGrowthPercent,
                            totalGrowthPositive = uiState.totalGrowthPositive,
                        )
                    }

                    // 4 stat cards (2×2 grid)
                    item {
                        val filterMonths = when (uiState.selectedFilter) {
                            TimeFilter.ONE_MONTH     -> 1
                            TimeFilter.THREE_MONTHS  -> 3
                            TimeFilter.SIX_MONTHS    -> 6
                            TimeFilter.TWELVE_MONTHS -> 12
                            TimeFilter.ALL           -> uiState.monthlyEntries.size
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AnalyticsSummaryCard(
                                    label    = stringResource(R.string.analytics_avg_month),
                                    value    = uiState.avgMonthlyGrowth,
                                    subLabel = stringResource(R.string.analytics_across_months, filterMonths),
                                    modifier = Modifier.weight(1f)
                                )
                                AnalyticsSummaryCard(
                                    label      = stringResource(R.string.analytics_best_month),
                                    value      = uiState.bestMonthValue,
                                    subLabel   = uiState.bestMonthLabel,
                                    valueColor = PositiveGreen,
                                    modifier   = Modifier.weight(1f)
                                )
                            }
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AnalyticsSummaryCard(
                                    label    = stringResource(R.string.analytics_hardest_month),
                                    value    = uiState.worstMonthValue,
                                    subLabel = uiState.worstMonthLabel,
                                    modifier = Modifier.weight(1f)
                                )
                                AnalyticsSummaryCard(
                                    label      = stringResource(R.string.analytics_total_growth_label),
                                    value      = uiState.totalGrowthPercent,
                                    subLabel   = stringResource(R.string.analytics_this_period),
                                    valueColor = if (uiState.totalGrowthPositive) PositiveGreen else MaterialTheme.colorScheme.error,
                                    modifier   = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // ── EXPLORE section ──────────────────────────────────────
                    item {
                        Text(
                            text = stringResource(R.string.analytics_explore),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                        )
                    }

                    item { FuturePredictionEntryCard(onClick = onNavigateToPrediction) }

                    item {
                        GoalCalculatorEntryCard(
                            onClick = onNavigateToGoal,
                            currentNetWorthFormatted = uiState.currentNetWorthFormatted,
                            targetAmountFormatted    = uiState.targetAmountFormatted,
                            goalProgressPercent      = uiState.goalProgressPercent,
                            hasGoal                  = uiState.hasGoal,
                        )
                    }

                    item {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            YearlyRecapEntryCard(onClick = onNavigateToRecap, modifier = Modifier.weight(1f))
                            ComparePeriodsEntryCard(onClick = onNavigateToCompare, modifier = Modifier.weight(1f))
                        }
                    }

                    // Account comparison (multi-account)
                    if (uiState.accountComparison.size >= 2) {
                        item { AccountComparisonCard(lines = uiState.accountComparison) }
                    }

                    // ── Monthly change bar chart ──────────────────────────────
                    if (uiState.monthlyEntries.size >= 2) {
                        item {
                            Column {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.analytics_monthly_change),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = uiState.filterPeriodLabel.lowercase().removePrefix("total · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                MonthlyChangeBarChart(entries = uiState.monthlyEntries)
                            }
                        }
                    }

                    // ── Month-by-month section (preview, full list in AllMonths) ──
                    val nonFirstEntries = uiState.monthlyEntries.filter { !it.isFirst }
                    if (nonFirstEntries.isNotEmpty()) {
                        item {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.analytics_month_by_month),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                TextButton(onClick = onNavigateToAllMonths) {
                                    Text(
                                        text = stringResource(R.string.analytics_see_all),
                                        color = PositiveGreen,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = PositiveGreen,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                        item {
                            val previewEntries = nonFirstEntries.take(3)
                            Surface(
                                shape    = RoundedCornerShape(20.dp),
                                color    = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column {
                                    previewEntries.forEachIndexed { i, entry ->
                                        MonthByMonthRow(
                                            entry           = entry,
                                            transitionLabel = entry.transitionLabel,
                                            rangeLabel      = entry.rangeLabel,
                                            showDivider     = i < previewEntries.lastIndex,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Filter bottom sheet (triggered by the "6M ▼" chip)
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FilterBottomSheet(
                selectedFilter = uiState.selectedFilter,
                onFilterSelect = { filter ->
                    scope.launch {
                        sheetState.hide()
                        showFilterSheet = false
                        onIntent(AnalyticsIntent.SelectFilter(filter))
                    }
                }
            )
        }
    }
}

// ── Filter bottom sheet ───────────────────────────────────────────────────

@Composable
private fun FilterBottomSheet(
    selectedFilter: TimeFilter,
    onFilterSelect: (TimeFilter) -> Unit
) {
    val options = listOf(
        TimeFilter.ONE_MONTH     to stringResource(R.string.filter_label_3m).replace("3", "1"),
        TimeFilter.THREE_MONTHS  to stringResource(R.string.filter_label_3m),
        TimeFilter.SIX_MONTHS    to stringResource(R.string.filter_label_6m),
        TimeFilter.TWELVE_MONTHS to stringResource(R.string.filter_label_12m),
        TimeFilter.ALL           to stringResource(R.string.filter_label_all),
    )

    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = stringResource(R.string.analytics_filter_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column {
                options.forEachIndexed { index, (filter, label) ->
                    val isSelected = filter == selectedFilter
                    Surface(
                        onClick = { onFilterSelect(filter) },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { onFilterSelect(filter) },
                                colors = RadioButtonDefaults.colors(selectedColor = PositiveGreen)
                            )
                        }
                    }
                    if (index < options.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

// ── Period label helpers ──────────────────────────────────────────────────

@Composable
private fun TimeFilter.toShortLabel(): String = when (this) {
    TimeFilter.ONE_MONTH     -> stringResource(R.string.filter_1m)
    TimeFilter.THREE_MONTHS  -> stringResource(R.string.filter_3m)
    TimeFilter.SIX_MONTHS    -> stringResource(R.string.filter_6m)
    TimeFilter.TWELVE_MONTHS -> stringResource(R.string.filter_1y)
    TimeFilter.ALL           -> stringResource(R.string.filter_all)
}

// ── Monthly change bar chart ──────────────────────────────────────────────

@Composable
private fun MonthlyChangeBarChart(entries: List<MonthlyEntryUiModel>) {
    data class BarData(
        val displayDiff: Double,
        val isPositive: Boolean,
        val formattedDisplayDiff: String,
        val formattedEventAmount: String,
        val hasAccountAddition: Boolean,
        val hasAccountRemoval: Boolean,
        val monthLabel: String,   // "May"
        val yearLabel: String,    // "26"  — shown on a second line
    )

    val bars = buildList {
        entries.forEachIndexed { i, entry ->
            val prev = entries.getOrNull(i + 1) ?: return@forEachIndexed
            val rawDiff     = entry.rawValue - prev.rawValue
            val displayDiff = entry.organicDiff ?: rawDiff
            val parts       = entry.monthLabel.split(" ")
            add(BarData(
                displayDiff          = displayDiff,
                isPositive           = displayDiff >= 0,
                formattedDisplayDiff = entry.formattedOrganicDiff.ifEmpty { entry.formattedDiff },
                formattedEventAmount = entry.formattedEventAmount,
                hasAccountAddition   = entry.hasAccountAddition,
                hasAccountRemoval    = entry.hasAccountRemoval,
                monthLabel           = parts.firstOrNull()?.take(3) ?: "",
                yearLabel            = parts.lastOrNull()?.takeLast(2) ?: "",
            ))
        }
    }.reversed()

    if (bars.isEmpty()) return

    // Scale is based on organic diffs so a large account addition doesn't crush other bars
    val maxAbs = bars.maxOf { kotlin.math.abs(it.displayDiff) }.takeIf { it > 0 } ?: 1.0
    val posColor       = PositiveGreen
    val negColor       = Color(0xFFE8836A)
    val additionColor  = Color(0xFFFFC107)   // amber — account added
    val removalColor   = Color(0xFFE8836A)   // coral — account removed
    val hasAnyEvent    = bars.any { it.hasAccountAddition || it.hasAccountRemoval }
    var selectedBarIndex by remember(bars) { mutableStateOf<Int?>(null) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tooltip area — always reserves space to avoid layout jumps
            Box(modifier = Modifier.fillMaxWidth().height(if (hasAnyEvent) 36.dp else 20.dp)) {
                selectedBarIndex?.let { idx ->
                    val bar = bars[idx]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        Text(
                            text       = "${bar.monthLabel} '${bar.yearLabel}  ${bar.formattedDisplayDiff}",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (bar.isPositive) posColor else negColor,
                        )
                        when {
                            bar.hasAccountAddition && bar.formattedEventAmount.isNotEmpty() -> Text(
                                text     = "+${bar.formattedEventAmount} account added",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = additionColor,
                                fontSize = 9.sp,
                            )
                            bar.hasAccountRemoval && bar.formattedEventAmount.isNotEmpty() -> Text(
                                text     = "−${bar.formattedEventAmount} account removed",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = removalColor,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .pointerInput(bars) {
                        detectTapGestures { offset ->
                            val w    = size.width.toFloat()
                            val n    = bars.size
                            val barW = (w / n) * 0.55f
                            val gap  = (w / n) * 0.45f
                            val tapped = bars.indices.firstOrNull { i ->
                                val left = i * (barW + gap) + gap / 2f
                                offset.x >= left && offset.x <= left + barW
                            }
                            selectedBarIndex = if (tapped == selectedBarIndex) null else tapped
                        }
                    }
            ) {
                val n    = bars.size
                val w    = size.width
                val h    = size.height
                val barW = (w / n) * 0.55f
                val gap  = (w / n) * 0.45f
                val maxH = h * 0.82f  // leave headroom for markers

                bars.forEachIndexed { i, bar ->
                    val barH  = (kotlin.math.abs(bar.displayDiff) / maxAbs * maxH).toFloat().coerceAtLeast(4.dp.toPx())
                    val left  = i * (barW + gap) + gap / 2f
                    val alpha = if (selectedBarIndex == null || i == selectedBarIndex) 1f else 0.3f
                    drawRoundRect(
                        color        = (if (bar.isPositive) posColor else negColor).copy(alpha = alpha),
                        topLeft      = androidx.compose.ui.geometry.Offset(left, h - barH),
                        size         = androidx.compose.ui.geometry.Size(barW, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    )
                    // Event dot: amber = account added, coral = account removed
                    val eventColor = when {
                        bar.hasAccountAddition -> additionColor
                        bar.hasAccountRemoval  -> removalColor
                        else                   -> null
                    }
                    if (eventColor != null) {
                        val markerR = 3.5.dp.toPx()
                        val markerX = left + barW / 2f
                        val markerY = (h - barH - markerR - 4.dp.toPx()).coerceAtLeast(markerR)
                        drawCircle(
                            color  = eventColor.copy(alpha = alpha),
                            radius = markerR,
                            center = androidx.compose.ui.geometry.Offset(markerX, markerY),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                bars.forEach { bar ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text      = bar.monthLabel,
                            style     = MaterialTheme.typography.labelSmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize  = 9.sp,
                            textAlign = TextAlign.Center,
                        )
                        if (bar.yearLabel.isNotEmpty()) {
                            Text(
                                text      = bar.yearLabel,
                                style     = MaterialTheme.typography.labelSmall,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize  = 8.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Account filter row ────────────────────────────────────────────────────

@Composable
private fun AccountFilterRow(
    accounts: List<com.agcoding.networkapp.account.domain.model.Account>,
    selectedAccountId: Long?,
    onSelect: (Long?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val allSel = selectedAccountId == null
        Surface(
            onClick = { onSelect(null) }, shape = RoundedCornerShape(20.dp),
            color  = if (allSel) MaterialTheme.colorScheme.onSurface else Color.Transparent,
            border = if (!allSel) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) else null,
        ) {
            Text(
                text = stringResource(R.string.filter_all),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (allSel) FontWeight.Bold else FontWeight.Normal,
                color = if (allSel) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        accounts.forEach { account ->
            val isSel = account.id == selectedAccountId
            val ac = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
            Surface(
                onClick = { onSelect(account.id) }, shape = RoundedCornerShape(20.dp),
                color  = if (isSel) ac else Color.Transparent,
                border = if (!isSel) androidx.compose.foundation.BorderStroke(1.dp, ac.copy(alpha = 0.5f)) else null,
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (isSel) MaterialTheme.colorScheme.surface else ac))
                    Spacer(Modifier.width(6.dp))
                    Text(account.name, style = MaterialTheme.typography.labelLarge,
                         fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                         color = if (isSel) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun AnalyticsContentPreview() {
    NetWorthTheme {
        AnalyticsContent(
            uiState = AnalyticsUiState(
                isLoading = false, hasData = true,
                selectedFilter = TimeFilter.SIX_MONTHS,
                chartData = listOf(
                    ChartPoint(0f, 0.2f), ChartPoint(0.2f, 0.35f), ChartPoint(0.4f, 0.4f),
                    ChartPoint(0.6f, 0.55f), ChartPoint(0.8f, 0.72f), ChartPoint(1f, 0.9f)
                ),
                chartStartLabel = "Nov '25", chartMidLabel = "Feb '26", chartEndLabel = "May '26",
                currentNetWorthFormatted = "€18,200",
                filterPeriodLabel = "TOTAL · LAST 6 MONTHS",
                totalGrowth = "+€6,080", totalGrowthPercent = "+50.2%", totalGrowthPositive = true,
                avgMonthlyGrowth = "+€1,013", bestMonthLabel = "Dec", bestMonthValue = "+€1,460",
                worstMonthLabel = "Jan", worstMonthValue = "€680",
                hasGoal = true, targetAmountFormatted = "€100,000", goalProgressPercent = 18,
                monthlyEntries = listOf(
                    MonthlyEntryUiModel("May 2026", "€18,200", "+€850", "+4.9%", true, false, 18200.0),
                    MonthlyEntryUiModel("Apr 2026", "€17,350", "+€870", "+5.3%", true, false, 17350.0),
                    MonthlyEntryUiModel("Nov 2025", "€12,120", "—", "", true, true, 12120.0),
                )
            ),
            onIntent = {}, onNavigateToAllMonths = {}, onNavigateToPrediction = {},
            onNavigateToGoal = {}, onNavigateToRecap = {}, onNavigateToCompare = {}
        )
    }
}
