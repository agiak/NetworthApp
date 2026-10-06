package com.agcoding.networkapp.savings.presentation

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.savings.presentation.components.SavingsAccountCard
import com.agcoding.networkapp.savings.presentation.components.SavingsSummaryCard
import com.agcoding.networkapp.savings.presentation.model.SavingsAccountUiModel
import com.agcoding.networkapp.savings.presentation.model.SavingsSummaryUiModel
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen
import com.agcoding.networkapp.shared.ui.utils.ThousandSeparatorTransformation

@Composable
fun SavingsPlannerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFixedExpenses: () -> Unit,
    viewModel: SavingsPlannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SavingsPlannerContent(
        uiState = uiState,
        onIntent = { intent ->
            when (intent) {
                SavingsPlannerIntent.NavigateToFixedExpenses -> onNavigateToFixedExpenses()
                else -> viewModel.onIntent(intent)
            }
        },
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsPlannerContent(
    uiState: SavingsPlannerUiState,
    onIntent: (SavingsPlannerIntent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(SavingsPlannerIntent.ClearError)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.savings_planner_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.savings_planner_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                // Household total only makes sense with more than one account
                if (uiState.accounts.size > 1 && uiState.summary != null) {
                    item { SavingsSummaryCard(summary = uiState.summary) }
                }

                item {
                    ManageFixedExpensesButton(
                        expenseCount = uiState.totalExpenseCount,
                        onClick = { onIntent(SavingsPlannerIntent.NavigateToFixedExpenses) },
                    )
                }

                if (uiState.accounts.size > 1) {
                    item {
                        Text(
                            text = stringResource(R.string.savings_planner_per_person).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                        )
                    }
                }

                items(uiState.accounts, key = { it.accountId }) { account ->
                    SavingsAccountCard(
                        account = account,
                        onEditSalary = { onIntent(SavingsPlannerIntent.EditSalary(account.accountId)) },
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.savings_planner_split_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    uiState.editingAccount?.let { account ->
        SalaryDialog(
            accountName = account.accountName,
            salaryInput = uiState.salaryInput,
            currencySymbol = uiState.currencySymbol,
            isSaving = uiState.isSaving,
            onValueChange = { onIntent(SavingsPlannerIntent.UpdateSalaryInput(it)) },
            onSave = { onIntent(SavingsPlannerIntent.SaveSalary) },
            onDismiss = { onIntent(SavingsPlannerIntent.DismissSalaryDialog) },
        )
    }
}

@Composable
private fun ManageFixedExpensesButton(
    expenseCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "💸", fontSize = 20.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.savings_planner_manage_expenses),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (expenseCount == 0) stringResource(R.string.savings_planner_no_expenses)
                           else stringResource(R.string.savings_planner_expenses_declared, expenseCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SalaryDialog(
    accountName: String,
    salaryInput: String,
    currencySymbol: String,
    isSaving: Boolean,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.savings_planner_salary_dialog_title),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            OutlinedTextField(
                value = salaryInput,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label = { Text(stringResource(R.string.savings_planner_salary_hint)) },
                leadingIcon = {
                    Text(
                        text = currencySymbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PositiveGreen,
                    )
                },
                singleLine = true,
                visualTransformation = ThousandSeparatorTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PositiveGreen,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    cursorColor = PositiveGreen,
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !isSaving) {
                Text(
                    text = stringResource(R.string.fixed_expense_btn_save),
                    color = PositiveGreen,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun SavingsPlannerContentPreview() {
    NetWorthTheme {
        SavingsPlannerContent(
            uiState = SavingsPlannerUiState(
                isLoading = false,
                totalExpenseCount = 4,
                summary = SavingsSummaryUiModel(
                    formattedSalary        = "€3,500.00",
                    formattedFixedExpenses = "€1,140.00",
                    formattedSavings       = "€2,360.00",
                    formattedYearlySavings = "€28,320.00",
                    savingsRatePercent     = 67,
                    savingsShare           = 0.67f,
                    isDeficit              = false,
                    hasSalary              = true,
                ),
                accounts = listOf(
                    SavingsAccountUiModel(1, "Anastasis", "#76C893", 2_000.0, true, 3, "€2,000.00", "€720.00", "€1,280.00", "€15,360.00", 64, 0.64f, false),
                    SavingsAccountUiModel(2, "Xristina", "#A78BFA", 1_500.0, true, 2, "€1,500.00", "€420.00", "€1,080.00", "€12,960.00", 72, 0.72f, false),
                ),
            ),
            onIntent = {},
            onNavigateBack = {},
        )
    }
}
