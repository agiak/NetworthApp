package com.agcoding.networkapp.expenseanalysis.presentation

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.expenseanalysis.domain.model.CategorySource
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory
import com.agcoding.networkapp.expenseanalysis.presentation.components.CategoryCard
import com.agcoding.networkapp.expenseanalysis.presentation.components.CategoryDonut
import com.agcoding.networkapp.expenseanalysis.presentation.components.CategoryPickerSheet
import com.agcoding.networkapp.expenseanalysis.presentation.model.AccountAmountUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.AccountFilterUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.AnalysisSummaryUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.AnalyzedExpenseUiModel
import com.agcoding.networkapp.expenseanalysis.presentation.model.CategoryUiModel
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun ExpenseAnalysisScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFixedExpenses: () -> Unit,
    viewModel: ExpenseAnalysisViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExpenseAnalysisContent(
        uiState = uiState,
        onIntent = { intent ->
            if (intent == ExpenseAnalysisIntent.NavigateToFixedExpenses) onNavigateToFixedExpenses()
            else viewModel.onIntent(intent)
        },
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseAnalysisContent(
    uiState: ExpenseAnalysisUiState,
    onIntent: (ExpenseAnalysisIntent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(ExpenseAnalysisIntent.ClearError)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expense_analysis_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            !uiState.hasAnyExpenses -> EmptyState(
                onAddExpenses = { onIntent(ExpenseAnalysisIntent.NavigateToFixedExpenses) },
                modifier = Modifier.fillMaxSize().padding(paddingValues),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.accounts.size > 1) {
                    item {
                        AccountSelector(
                            accounts = uiState.accounts,
                            selectedAccountId = uiState.selectedAccountId,
                            onSelect = { onIntent(ExpenseAnalysisIntent.SelectAccount(it)) },
                        )
                    }
                }

                uiState.summary?.let { summary ->
                    item { SummaryCard(summary) }
                }

                if (uiState.uncertainCount > 0) {
                    item { UncertainHint(uiState.uncertainCount) }
                }

                if (uiState.categories.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.expense_analysis_no_expenses_for_account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }
                }

                items(uiState.categories, key = { it.category.name }) { category ->
                    CategoryCard(
                        category = category,
                        isExpanded = category.category in uiState.expandedCategories,
                        onToggle = { onIntent(ExpenseAnalysisIntent.ToggleCategory(category.category)) },
                        onExpenseClick = { onIntent(ExpenseAnalysisIntent.EditCategory(it)) },
                    )
                }

                item { HowItWorks(uiState.learnedChoicesCount) }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    uiState.editingExpense?.let { expense ->
        CategoryPickerSheet(
            expense = expense,
            onPick = { onIntent(ExpenseAnalysisIntent.PickCategory(it)) },
            onResetToAutomatic = { onIntent(ExpenseAnalysisIntent.ResetToAutomatic) },
            onDismiss = { onIntent(ExpenseAnalysisIntent.DismissPicker) },
        )
    }
}

@Composable
private fun AccountSelector(
    accounts: List<AccountFilterUiModel>,
    selectedAccountId: Long?,
    onSelect: (Long?) -> Unit,
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.onSurface,
        selectedLabelColor = MaterialTheme.colorScheme.surface,
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedAccountId == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.expense_analysis_all_accounts)) },
            colors = chipColors,
        )
        accounts.forEach { account ->
            FilterChip(
                selected = selectedAccountId == account.id,
                onClick = { onSelect(account.id) },
                label = { Text(account.name) },
                colors = chipColors,
            )
        }
    }
}

@Composable
private fun SummaryCard(summary: AnalysisSummaryUiModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CategoryDonut(
                slices = summary.slices,
                centerTitle = summary.formattedMonthly,
                centerSubtitle = stringResource(R.string.expense_analysis_per_month),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.expense_analysis_per_year, summary.formattedYearly),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = pluralStringResource(R.plurals.expense_analysis_expense_count, summary.expenseCount, summary.expenseCount),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
        }
    }
}

@Composable
private fun UncertainHint(count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🤔", fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = pluralStringResource(R.plurals.expense_analysis_uncertain, count, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HowItWorks(learnedChoicesCount: Int) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = stringResource(R.string.expense_analysis_how_it_works),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (learnedChoicesCount > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "🧠 " + pluralStringResource(R.plurals.expense_analysis_learned_count, learnedChoicesCount, learnedChoicesCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EmptyState(onAddExpenses: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "📊", fontSize = 48.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.expense_analysis_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.expense_analysis_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAddExpenses,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
            ),
        ) {
            Text(stringResource(R.string.expense_analysis_go_to_expenses), fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpenseAnalysisContentPreview() {
    val housing = listOf(
        AnalyzedExpenseUiModel(1, "House", "€350.00", null, ExpenseCategory.HOUSING, CategorySource.AUTO, false),
        AnalyzedExpenseUiModel(2, "Electricity", "€75.00", null, ExpenseCategory.HOUSING, CategorySource.AUTO, false),
    )
    val subscriptions = listOf(
        AnalyzedExpenseUiModel(3, "Gym", "€48.33", null, ExpenseCategory.SUBSCRIPTIONS, CategorySource.MANUAL, false),
        AnalyzedExpenseUiModel(4, "Claude", "€18.00", null, ExpenseCategory.SUBSCRIPTIONS, CategorySource.LEARNED, false),
    )
    NetWorthTheme {
        ExpenseAnalysisContent(
            uiState = ExpenseAnalysisUiState(
                isLoading = false,
                hasAnyExpenses = true,
                accounts = listOf(AccountFilterUiModel(1, "Anastasis", "#76C893"), AccountFilterUiModel(2, "Xristina", "#A78BFA")),
                summary = AnalysisSummaryUiModel(
                    formattedMonthly = "€736.67",
                    formattedYearly = "€8,840.00",
                    expenseCount = 4,
                    slices = listOf(ExpenseCategory.HOUSING to 0.6f, ExpenseCategory.SUBSCRIPTIONS to 0.25f, ExpenseCategory.TRANSPORT to 0.15f),
                ),
                categories = listOf(
                    CategoryUiModel(ExpenseCategory.HOUSING, "€425.00", 58, 0.58f, housing,
                        listOf(AccountAmountUiModel("Anastasis", "#76C893", "€212.50"), AccountAmountUiModel("Xristina", "#A78BFA", "€212.50"))),
                    CategoryUiModel(ExpenseCategory.SUBSCRIPTIONS, "€66.33", 9, 0.09f, subscriptions, emptyList()),
                ),
                uncertainCount = 1,
                learnedChoicesCount = 2,
                expandedCategories = setOf(ExpenseCategory.HOUSING),
            ),
            onIntent = {},
            onNavigateBack = {},
        )
    }
}
