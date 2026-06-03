package com.agcoding.networkapp.analytics.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.analytics.presentation.components.MonthByMonthRow
import com.agcoding.networkapp.analytics.presentation.model.MonthlyEntryUiModel
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen
import kotlinx.coroutines.launch

@Composable
fun AllMonthsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AllMonthsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AllMonthsContent(uiState = uiState, onIntent = viewModel::onIntent, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllMonthsContent(
    uiState: AllMonthsUiState,
    onIntent: (AllMonthsIntent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.analytics_month_by_month),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    // Period filter chip
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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    // Multi-select account filter
                    if (uiState.accounts.size > 1) {
                        MultiAccountFilterRow(
                            accounts           = uiState.accounts,
                            selectedAccountIds = uiState.selectedAccountIds,
                            onToggle           = { onIntent(AllMonthsIntent.ToggleAccount(it)) },
                            onClearAll         = { onIntent(AllMonthsIntent.ClearAccountFilter) },
                            modifier           = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    // Sort filter chips
                    SortFilterRow(
                        selectedSort = uiState.sortOrder,
                        onSelect     = { onIntent(AllMonthsIntent.SelectSort(it)) },
                        modifier     = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )

                    if (uiState.monthlyEntries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.analytics_no_data),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            item {
                                Surface(
                                    shape    = RoundedCornerShape(20.dp),
                                    color    = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column {
                                        val isTimeSorted = uiState.sortOrder == AllMonthsSortOrder.NEWEST_FIRST
                                                        || uiState.sortOrder == AllMonthsSortOrder.OLDEST_FIRST
                                        uiState.monthlyEntries.forEachIndexed { index, entry ->
                                            MonthByMonthRow(
                                                entry           = entry,
                                                transitionLabel = if (isTimeSorted) entry.transitionLabel else "",
                                                rangeLabel      = if (isTimeSorted) entry.rangeLabel else entry.formattedValue,
                                                showDivider     = index < uiState.monthlyEntries.lastIndex,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Period filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            PeriodFilterSheet(
                selectedFilter = uiState.selectedFilter,
                onSelect = { filter ->
                    scope.launch {
                        sheetState.hide()
                        showFilterSheet = false
                        onIntent(AllMonthsIntent.SelectFilter(filter))
                    }
                },
            )
        }
    }
}

// ── Multi-select account filter ───────────────────────────────────────────

@Composable
private fun MultiAccountFilterRow(
    accounts: List<Account>,
    selectedAccountIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val allSelected = selectedAccountIds.isEmpty()
        Surface(
            onClick = onClearAll,
            shape = RoundedCornerShape(20.dp),
            color = if (allSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
            border = if (!allSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) else null,
        ) {
            Text(
                text = stringResource(R.string.filter_all),
                style = MaterialTheme.typography.labelLarge,
                color = if (allSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        accounts.forEach { account ->
            val isSelected = account.id in selectedAccountIds
            val accentColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) }
                              catch (e: Exception) { MaterialTheme.colorScheme.primary }
            Surface(
                onClick = { onToggle(account.id) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) accentColor else Color.Transparent,
                border = if (!isSelected) BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)) else null,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else accentColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

// ── Sort filter chips ─────────────────────────────────────────────────────

@Composable
private fun SortFilterRow(
    selectedSort: AllMonthsSortOrder,
    onSelect: (AllMonthsSortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AllMonthsSortOrder.entries.forEach { order ->
            val isSelected = order == selectedSort
            Surface(
                onClick = { onSelect(order) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) else null,
            ) {
                Text(
                    text = sortLabel(order),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

// ── Period filter bottom sheet ────────────────────────────────────────────

@Composable
private fun PeriodFilterSheet(
    selectedFilter: TimeFilter,
    onSelect: (TimeFilter) -> Unit,
) {
    val options = listOf(
        TimeFilter.ONE_MONTH     to stringResource(R.string.filter_label_1m),
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
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Column {
                options.forEachIndexed { index, (filter, label) ->
                    val isSelected = filter == selectedFilter
                    Surface(
                        onClick = { onSelect(filter) },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(filter) },
                                colors = RadioButtonDefaults.colors(selectedColor = PositiveGreen),
                            )
                        }
                    }
                    if (index < options.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────

@Composable
private fun sortLabel(order: AllMonthsSortOrder): String = when (order) {
    AllMonthsSortOrder.NEWEST_FIRST  -> stringResource(R.string.sort_newest_first)
    AllMonthsSortOrder.OLDEST_FIRST  -> stringResource(R.string.sort_oldest_first)
    AllMonthsSortOrder.HIGHEST_VALUE -> stringResource(R.string.sort_highest_value)
    AllMonthsSortOrder.LOWEST_VALUE  -> stringResource(R.string.sort_lowest_value)
}

@Composable
private fun TimeFilter.toShortLabel(): String = when (this) {
    TimeFilter.ONE_MONTH     -> stringResource(R.string.filter_1m)
    TimeFilter.THREE_MONTHS  -> stringResource(R.string.filter_3m)
    TimeFilter.SIX_MONTHS    -> stringResource(R.string.filter_6m)
    TimeFilter.TWELVE_MONTHS -> stringResource(R.string.filter_1y)
    TimeFilter.ALL           -> stringResource(R.string.filter_all)
}

// ── Preview ───────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun AllMonthsPreview() {
    NetWorthTheme {
        AllMonthsContent(
            uiState = AllMonthsUiState(
                isLoading = false,
                accounts = listOf(
                    Account(1, "Savings", 0.0, "#4CAF50"),
                    Account(2, "Investments", 0.0, "#2196F3"),
                ),
                selectedAccountIds = emptySet(),
                selectedFilter = TimeFilter.ALL,
                monthlyEntries = listOf(
                    MonthlyEntryUiModel("May 2026",      "€65,855", "+€4,873", "+8.0%", true,  false),
                    MonthlyEntryUiModel("April 2026",    "€60,982", "-€1,264", "-2.0%", false, false),
                    MonthlyEntryUiModel("March 2026",    "€62,246", "+€4,497", "+7.8%", true,  false),
                    MonthlyEntryUiModel("February 2026", "€57,749", "+€4,208", "+7.9%", true,  false),
                    MonthlyEntryUiModel("January 2026",  "€53,541", "—",       "",      true,  true),
                ),
            ),
            onIntent = {},
            onNavigateBack = {},
        )
    }
}
