package com.agcoding.networkapp.fixedexpenses.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpenseSortOption
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.presentation.components.AccountStatsCard
import com.agcoding.networkapp.fixedexpenses.presentation.components.FixedExpenseBottomSheet
import com.agcoding.networkapp.fixedexpenses.presentation.components.FixedExpenseItem
import com.agcoding.networkapp.fixedexpenses.presentation.model.AccountExpenseStatsUiModel
import com.agcoding.networkapp.fixedexpenses.presentation.model.FixedExpenseUiModel
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun FixedExpensesScreen(
    onNavigateBack: () -> Unit,
    viewModel: FixedExpensesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FixedExpensesContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FixedExpensesContent(
    uiState: FixedExpensesUiState,
    onIntent: (FixedExpensesIntent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterSheet by remember { mutableStateOf(false) }
    val activeFilterCount = uiState.filterAccountIds.size

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(FixedExpensesIntent.ClearError)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.fixed_expenses_title), fontWeight = FontWeight.Bold)
                        if (uiState.expenses.isNotEmpty() || uiState.filterAccountIds.isNotEmpty()) {
                            Text(
                                text = uiState.sortOption.toLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (uiState.availableAccounts.isNotEmpty() || uiState.expenses.isNotEmpty()) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (activeFilterCount > 0) {
                                        Badge { Text(activeFilterCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.fixed_expense_filter_title),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            if (uiState.expenses.isNotEmpty() || uiState.filterAccountIds.isNotEmpty()) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = { onIntent(FixedExpensesIntent.ShowAddSheet) },
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            uiState.expenses.isEmpty() && uiState.filterAccountIds.isEmpty() -> {
                FixedExpensesEmptyState(
                    onAddExpense = { onIntent(FixedExpensesIntent.ShowAddSheet) },
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        TotalCard(
                            monthlyTotal = uiState.totalFormatted,
                            yearlyTotal = uiState.yearlyFormatted,
                            count = uiState.expenses.size,
                        )
                    }
                    if (uiState.accountStats.isNotEmpty()) {
                        item {
                            AccountStatsCard(stats = uiState.accountStats)
                        }
                    }
                    if (uiState.expenses.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.fixed_expense_filter_no_results),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        items(uiState.expenses, key = { it.id }) { expense ->
                            FixedExpenseItem(
                                expense = expense,
                                onClick = { onIntent(FixedExpensesIntent.ShowEditSheet(expense)) },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (uiState.isSheetVisible) {
        FixedExpenseBottomSheet(
            uiState = uiState,
            onTitleChange = { onIntent(FixedExpensesIntent.UpdateTitle(it)) },
            onNoteChange = { onIntent(FixedExpensesIntent.UpdateNote(it)) },
            onCostChange = { onIntent(FixedExpensesIntent.UpdateCost(it)) },
            onDateChange = { onIntent(FixedExpensesIntent.UpdateDate(it)) },
            onRecurrenceChange = { onIntent(FixedExpensesIntent.UpdateRecurrence(it)) },
            onToggleAccount = { onIntent(FixedExpensesIntent.ToggleAccount(it)) },
            onSelectAllAccounts = { onIntent(FixedExpensesIntent.SelectAllAccounts) },
            onSave = { onIntent(FixedExpensesIntent.Save) },
            onDelete = { uiState.editingExpense?.let { onIntent(FixedExpensesIntent.Delete(it.id)) } },
            onDismiss = { onIntent(FixedExpensesIntent.HideSheet) },
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            sortOption = uiState.sortOption,
            filterAccountIds = uiState.filterAccountIds,
            availableAccounts = uiState.availableAccounts,
            onSortSelect = { onIntent(FixedExpensesIntent.SetSortOption(it)) },
            onToggleFilterAccount = { onIntent(FixedExpensesIntent.ToggleFilterAccount(it)) },
            onClearFilterAccounts = { onIntent(FixedExpensesIntent.ClearFilterAccounts) },
            onDismiss = { showFilterSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheet(
    sortOption: FixedExpenseSortOption,
    filterAccountIds: Set<Long>,
    availableAccounts: List<Account>,
    onSortSelect: (FixedExpenseSortOption) -> Unit,
    onToggleFilterAccount: (Long) -> Unit,
    onClearFilterAccounts: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sortOptions = listOf(
        FixedExpenseSortOption.COST_HIGH to stringResource(R.string.fixed_expense_sort_cost_high),
        FixedExpenseSortOption.COST_LOW  to stringResource(R.string.fixed_expense_sort_cost_low),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.fixed_expense_filter_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )

            // Sort section
            Text(
                text = stringResource(R.string.fixed_expense_sort_section_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column {
                    sortOptions.forEachIndexed { index, (option, label) ->
                        val isSelected = option == sortOption
                        Surface(
                            onClick = { onSortSelect(option) },
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
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
                                    onClick = { onSortSelect(option) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PositiveGreen),
                                )
                            }
                        }
                        if (index < sortOptions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            // Account filter section (only if accounts exist)
            if (availableAccounts.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.fixed_expense_filter_account_section_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp,
                    )
                    if (filterAccountIds.isNotEmpty()) {
                        Surface(
                            onClick = onClearFilterAccounts,
                            color = Color.Transparent,
                        ) {
                            Text(
                                text = stringResource(R.string.fixed_expense_filter_clear),
                                style = MaterialTheme.typography.labelSmall,
                                color = PositiveGreen,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availableAccounts.forEach { account ->
                        val isSelected = account.id in filterAccountIds
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleFilterAccount(account.id) },
                            label = { Text(account.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                                selectedLabelColor = MaterialTheme.colorScheme.surface,
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TotalCard(
    monthlyTotal: String,
    yearlyTotal: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.fixed_expenses_total_monthly).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = monthlyTotal,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PositiveGreen,
                    )
                }
                VerticalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxHeight(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.fixed_expenses_total_yearly).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = yearlyTotal,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.fixed_expenses_count, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FixedExpensesEmptyState(
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "💸", fontSize = 48.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.fixed_expenses_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.fixed_expenses_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAddExpense,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
            ),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.fixed_expenses_add),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun FixedExpenseSortOption.toLabel(): String = when (this) {
    FixedExpenseSortOption.COST_HIGH -> stringResource(R.string.fixed_expense_sort_cost_high)
    FixedExpenseSortOption.COST_LOW  -> stringResource(R.string.fixed_expense_sort_cost_low)
}

@Preview(showBackground = true)
@Composable
private fun FixedExpensesContentPreview() {
    NetWorthTheme {
        FixedExpensesContent(
            uiState = FixedExpensesUiState(
                isLoading = false,
                totalFormatted = "€1,850.00",
                expenses = listOf(
                    FixedExpenseUiModel(1, "Rent", "Monthly", "€1,200.00 / mo", 1200.0, null, RecurrenceType.MONTHLY, null),
                    FixedExpenseUiModel(2, "Netflix", "Subscription", "€15.99 / mo", 15.99, "1 Jun 2025", RecurrenceType.MONTHLY, null),
                    FixedExpenseUiModel(3, "Car Insurance", "Annual", "€600.00 / yr", 600.0, "1 Jan 2026", RecurrenceType.ANNUAL, "€50.00 / mo"),
                ),
                availableAccounts = listOf(Account(1, "Main"), Account(2, "Savings")),
                accountStats = listOf(
                    AccountExpenseStatsUiModel(1, "Main", "#76C893", 2, "€1,215.99"),
                    AccountExpenseStatsUiModel(2, "Savings", "#5B8DEF", 1, "€50.00"),
                ),
            ),
            onIntent = {},
            onNavigateBack = {},
        )
    }
}
