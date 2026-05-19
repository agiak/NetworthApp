package com.agcoding.networkapp.analytics.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.analytics.presentation.components.MonthByMonthRow
import com.agcoding.networkapp.analytics.presentation.model.MonthlyEntryUiModel
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.analytics_all_months_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
            else -> {
                Column(Modifier.fillMaxSize().padding(paddingValues)) {
                if (uiState.accounts.size > 1) {
                    AccountFilterRow(
                        accounts = uiState.accounts,
                        selectedAccountId = uiState.selectedAccountId,
                        onSelect = { onIntent(AllMonthsIntent.SelectAccount(it)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                uiState.monthlyEntries.forEachIndexed { index, entry ->
                                    MonthByMonthRow(
                                        entry = entry,
                                        showDivider = index < uiState.monthlyEntries.lastIndex
                                    )
                                }
                            }
                        }
                    }
                }
                } // end Column
            }
        }
    }
}

@Composable
private fun AccountFilterRow(
    accounts: List<com.agcoding.networkapp.account.domain.model.Account>,
    selectedAccountId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val allSelected = selectedAccountId == null
        Surface(
            onClick = { onSelect(null) },
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
            val isSelected  = account.id == selectedAccountId
            val accentColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) }
                              catch (e: Exception) { MaterialTheme.colorScheme.primary }
            Surface(
                onClick = { onSelect(account.id) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) accentColor else Color.Transparent,
                border = if (!isSelected) BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)) else null,
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AllMonthsPreview() {
    NetWorthTheme {
        AllMonthsContent(
            uiState = AllMonthsUiState(
                isLoading = false,
                monthlyEntries = listOf(
                    MonthlyEntryUiModel("May 2026", "€65,855", "+€4,873", "+8.0%", true, false),
                    MonthlyEntryUiModel("April 2026", "€60,982", "-€1,264", "-2.0%", false, false),
                    MonthlyEntryUiModel("March 2026", "€62,246", "+€4,497", "+7.8%", true, false),
                    MonthlyEntryUiModel("February 2026", "€57,749", "+€4,208", "+7.9%", true, false),
                    MonthlyEntryUiModel("January 2026", "€53,541", "—", "", true, true)
                )
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}
