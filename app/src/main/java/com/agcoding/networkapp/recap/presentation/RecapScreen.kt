package com.agcoding.networkapp.recap.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.recap.presentation.components.RecapGoalCard
import com.agcoding.networkapp.recap.presentation.components.RecapHeaderCard
import com.agcoding.networkapp.recap.presentation.components.RecapHighlightsGrid
import com.agcoding.networkapp.recap.presentation.components.RecapMonthByMonth
import com.agcoding.networkapp.recap.presentation.components.RecapTrendCard
import com.agcoding.networkapp.recap.presentation.components.RecapYearlyChart
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun RecapScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecapContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecapContent(
    uiState: RecapUiState,
    onIntent: (RecapIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(RecapIntent.ClearError)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.recap_title),
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Year selector
                    if (uiState.availableYears.size > 1) {
                        item {
                            YearSelector(
                                availableYears = uiState.availableYears,
                                selectedYear = uiState.selectedYear,
                                onYearSelect = { onIntent(RecapIntent.SelectYear(it)) }
                            )
                        }
                    }
                    // Account filter
                    if (uiState.accounts.size > 1) {
                        item {
                            RecapAccountFilterRow(
                                accounts = uiState.accounts,
                                selectedAccountId = uiState.selectedAccountId,
                                onSelect = { onIntent(RecapIntent.SelectAccount(it)) },
                            )
                        }
                    }

                    // Main content animates on year change
                    item {
                        AnimatedContent(
                            targetState = uiState.selectedYear,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "recapContent"
                        ) { _ ->
                            if (!uiState.hasData) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = stringResource(R.string.recap_no_data, uiState.selectedYear),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                RecapBody(uiState = uiState)
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
private fun RecapBody(uiState: RecapUiState) {
    androidx.compose.foundation.layout.Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RecapHeaderCard(
            year = uiState.selectedYear,
            totalGrowthFormatted = uiState.totalGrowthFormatted,
            totalGrowthPercent = uiState.totalGrowthPercent,
            totalGrowthPositive = uiState.totalGrowthPositive,
            startValue = uiState.startValue,
            endValue = uiState.endValue,
            monthsTracked = uiState.monthsTracked,
            isNewAllTimeHigh = uiState.isNewAllTimeHigh
        )

        RecapHighlightsGrid(
            bestMonthLabel = uiState.bestMonthLabel,
            bestMonthValue = uiState.bestMonthValue,
            worstMonthLabel = uiState.worstMonthLabel,
            worstMonthValue = uiState.worstMonthValue,
            avgMonthlyGrowth = uiState.avgMonthlyGrowth,
            biggestJump = uiState.biggestJump
        )

        if (uiState.hasGoal) {
            RecapGoalCard(
                goalProgress = uiState.goalProgress,
                goalProgressPercent = uiState.goalProgressPercent,
                goalYearContribution = uiState.goalYearContribution
            )
        }

        RecapTrendCard(trend = uiState.trend)

        if (uiState.chartData.size >= 2) {
            RecapYearlyChart(
                chartData = uiState.chartData,
                startLabel = uiState.chartStartLabel,
                endLabel = uiState.chartEndLabel
            )
        }

        RecapMonthByMonth(items = uiState.monthlyBreakdown)
    }
}

@Composable
private fun YearSelector(
    availableYears: List<Int>,
    selectedYear: Int,
    onYearSelect: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(availableYears) { year ->
            val isSelected = year == selectedYear
            Surface(
                onClick = { onYearSelect(year) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun RecapAccountFilterRow(
    accounts: List<com.agcoding.networkapp.account.domain.model.Account>,
    selectedAccountId: Long?,
    onSelect: (Long?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        item {
            val allSelected = selectedAccountId == null
            Surface(
                onClick = { onSelect(null) },
                shape = RoundedCornerShape(20.dp),
                color = if (allSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                border = if (!allSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) else null,
            ) {
                Text(
                    text = stringResource(R.string.filter_all),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (allSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        items(accounts) { account ->
            val isSelected = account.id == selectedAccountId
            val accentColor = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(account.colorHex)) }
                              catch (e: Exception) { MaterialTheme.colorScheme.primary }
            Surface(
                onClick = { onSelect(account.id) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) accentColor else Color.Transparent,
                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)) else null,
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecapContentPreview() {
    NetWorthTheme {
        RecapContent(
            uiState = RecapUiState(
                isLoading = false,
                hasData = true,
                availableYears = listOf(2026, 2025, 2024),
                selectedYear = 2025,
                totalGrowthFormatted = "+€18,400",
                totalGrowthPercent = "+24.5%",
                totalGrowthPositive = true,
                startValue = "€58,000",
                endValue = "€76,400",
                monthsTracked = 12,
                bestMonthLabel = "Mar",
                bestMonthValue = "+€3,200",
                worstMonthLabel = "Aug",
                worstMonthValue = "-€800",
                avgMonthlyGrowth = "+€1,250",
                biggestJump = "+€3,200",
                hasGoal = true,
                goalProgress = 0.42f,
                goalProgressPercent = "42%",
                goalYearContribution = "+€18,400",
                trend = RecapTrend.ACCELERATED,
                isNewAllTimeHigh = true
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}
