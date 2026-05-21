package com.agcoding.networkapp.analytics.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.agcoding.networkapp.analytics.presentation.components.TrendCard
import com.agcoding.networkapp.analytics.presentation.components.YearlyRecapEntryCard
import com.agcoding.networkapp.analytics.presentation.model.MonthlyEntryUiModel
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.components.ProjectionCard
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
                    Column {
                        Text(
                            text = stringResource(R.string.nav_analytics),
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.hasData) {
                            Text(
                                text = uiState.selectedFilter.toLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.hasData) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = stringResource(R.string.analytics_filter_title),
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
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            !uiState.hasData -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
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
                    if (uiState.accounts.size > 1) {
                        item {
                            AccountFilterRow(
                                accounts = uiState.accounts,
                                selectedAccountId = uiState.selectedAccountId,
                                onSelect = { onIntent(AnalyticsIntent.SelectAccount(it)) },
                            )
                        }
                    }

                    item {
                        AnalyticsChartCard(
                            chartData  = uiState.chartData,
                            startLabel = uiState.chartStartLabel,
                            midLabel   = uiState.chartMidLabel,
                            endLabel   = uiState.chartEndLabel,
                            topLabel   = uiState.highestNetWorth,
                            bottomLabel = uiState.lowestNetWorth,
                        )
                    }

                    if (uiState.accountComparison.size >= 2) {
                        item {
                            AccountComparisonCard(lines = uiState.accountComparison)
                        }
                    }

                    item {
                        TrendCard(
                            trendLabel = uiState.trendLabel,
                            trendDescription = uiState.trendDescription,
                            isPositive = uiState.trendIsPositive,
                            isNeutral = uiState.trendIsNeutral
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnalyticsSummaryCard(
                                label = stringResource(R.string.analytics_total_growth),
                                value = uiState.totalGrowth,
                                subLabel = uiState.totalGrowthPercent,
                                valueColor = if (uiState.totalGrowthPositive) PositiveGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsSummaryCard(
                                label = stringResource(R.string.analytics_avg_month),
                                value = uiState.avgMonthlyGrowth,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnalyticsSummaryCard(
                                label = stringResource(R.string.analytics_best_month),
                                value = uiState.bestMonthValue,
                                subLabel = uiState.bestMonthLabel,
                                valueColor = PositiveGreen,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsSummaryCard(
                                label = stringResource(R.string.analytics_worst_month),
                                value = uiState.worstMonthValue,
                                subLabel = uiState.worstMonthLabel,
                                valueColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnalyticsSummaryCard(
                                label = stringResource(R.string.analytics_highest_net_worth),
                                value = uiState.highestNetWorth,
                                subLabel = uiState.highestNetWorthDate,
                                valueColor = PositiveGreen,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsSummaryCard(
                                label = stringResource(R.string.analytics_lowest_net_worth),
                                value = uiState.lowestNetWorth,
                                subLabel = uiState.lowestNetWorthDate,
                                valueColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnalyticsSummaryCard(
                                label = stringResource(R.string.analytics_consistency),
                                value = uiState.consistencyPercent,
                                subLabel = uiState.consistencyDetail,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsSummaryCard(
                                label = stringResource(R.string.analytics_streak),
                                value = uiState.currentStreakLabel,
                                subLabel = uiState.currentStreakSubLabel,
                                valueColor = if (uiState.currentStreakLabel != "—") PositiveGreen else Color.Unspecified,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        ProjectionCard(
                            projectedNetWorth = uiState.projectedNetWorth,
                            projectedNetWorthDate = uiState.projectedNetWorthDate
                        )
                    }

                    item {
                        FuturePredictionEntryCard(onClick = onNavigateToPrediction)
                    }

                    item {
                        GoalCalculatorEntryCard(onClick = onNavigateToGoal)
                    }

                    item {
                        YearlyRecapEntryCard(onClick = onNavigateToRecap)
                    }

                    item {
                        ComparePeriodsEntryCard(onClick = onNavigateToCompare)
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.analytics_month_by_month),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.monthlyEntries.size > 5) {
                                TextButton(onClick = onNavigateToAllMonths) {
                                    Text(stringResource(R.string.analytics_show_all))
                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                                }
                            }
                        }
                    }

                    item {
                        val recentEntries = uiState.monthlyEntries.take(5)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                recentEntries.forEachIndexed { index, entry ->
                                    MonthByMonthRow(
                                        entry = entry,
                                        showDivider = index < recentEntries.lastIndex
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

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

@Composable
private fun FilterBottomSheet(
    selectedFilter: TimeFilter,
    onFilterSelect: (TimeFilter) -> Unit
) {
    val options = listOf(
        TimeFilter.THREE_MONTHS to stringResource(R.string.filter_label_3m),
        TimeFilter.SIX_MONTHS to stringResource(R.string.filter_label_6m),
        TimeFilter.TWELVE_MONTHS to stringResource(R.string.filter_label_12m),
        TimeFilter.ALL to stringResource(R.string.filter_label_all)
    )

    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = stringResource(R.string.analytics_filter_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
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
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PositiveGreen
                                )
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

@Composable
private fun AccountFilterRow(
    accounts: List<com.agcoding.networkapp.account.domain.model.Account>,
    selectedAccountId: Long?,
    onSelect: (Long?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "All" chip
        val allSelected = selectedAccountId == null
        Surface(
            onClick = { onSelect(null) },
            shape = RoundedCornerShape(20.dp),
            color = if (allSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
            border = if (!allSelected) androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ) else null,
        ) {
            Text(
                text = stringResource(R.string.filter_all),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (allSelected) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // Per-account chips
        accounts.forEach { account ->
            val isSelected = account.id == selectedAccountId
            val accentColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) }
                              catch (e: Exception) { MaterialTheme.colorScheme.primary }
            Surface(
                onClick = { onSelect(account.id) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) accentColor else Color.Transparent,
                border = if (!isSelected) androidx.compose.foundation.BorderStroke(
                    1.dp, accentColor.copy(alpha = 0.5f)
                ) else null,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else accentColor),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeFilter.toLabel(): String = when (this) {
    TimeFilter.THREE_MONTHS -> stringResource(R.string.filter_label_3m)
    TimeFilter.SIX_MONTHS -> stringResource(R.string.filter_label_6m)
    TimeFilter.TWELVE_MONTHS -> stringResource(R.string.filter_label_12m)
    TimeFilter.ALL -> stringResource(R.string.filter_label_all)
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsContentPreview() {
    NetWorthTheme {
        AnalyticsContent(
            uiState = AnalyticsUiState(
                isLoading = false,
                hasData = true,
                selectedFilter = TimeFilter.TWELVE_MONTHS,
                chartData = listOf(
                    ChartPoint(0f, 0.2f), ChartPoint(0.2f, 0.35f), ChartPoint(0.4f, 0.3f),
                    ChartPoint(0.6f, 0.55f), ChartPoint(0.8f, 0.7f), ChartPoint(1f, 0.9f)
                ),
                chartStartLabel = "May '25",
                chartMidLabel = "Nov '25",
                chartEndLabel = "May '26",
                totalGrowth = "+€8,200",
                totalGrowthPercent = "+45.0%",
                totalGrowthPositive = true,
                avgMonthlyGrowth = "+€683",
                bestMonthLabel = "Mar 2026",
                bestMonthValue = "+€2,100",
                worstMonthLabel = "Aug 2025",
                worstMonthValue = "-€300",
                highestNetWorth = "€26,200",
                highestNetWorthDate = "May 2026",
                lowestNetWorth = "€18,000",
                lowestNetWorthDate = "May 2025",
                trendLabel = "Increasing",
                trendDescription = "Net worth trending upward in this period",
                trendIsPositive = true,
                trendIsNeutral = false,
                consistencyPercent = "9/11",
                consistencyDetail = "months positive",
                projectedNetWorth = "€34,396",
                projectedNetWorthDate = "by May 2027",
                currentStreakLabel = "3",
                currentStreakSubLabel = "months in a row",
                monthlyEntries = listOf(
                    MonthlyEntryUiModel("May 2026", "€26,200", "+€1,200", "+4.8%", true, false),
                    MonthlyEntryUiModel("April 2026", "€25,000", "+€2,000", "+8.7%", true, false),
                    MonthlyEntryUiModel("March 2026", "€23,000", "+€3,000", "+15.0%", true, false),
                    MonthlyEntryUiModel("February 2026", "€20,000", "—", "", true, true)
                )
            ),
            onIntent = {},
            onNavigateToAllMonths = {},
            onNavigateToPrediction = {},
            onNavigateToGoal = {},
            onNavigateToRecap = {},
            onNavigateToCompare = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FilterBottomSheetPreview() {
    NetWorthTheme {
        FilterBottomSheet(
            selectedFilter = TimeFilter.TWELVE_MONTHS,
            onFilterSelect = {}
        )
    }
}
