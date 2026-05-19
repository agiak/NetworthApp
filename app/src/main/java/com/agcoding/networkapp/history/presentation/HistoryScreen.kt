package com.agcoding.networkapp.history.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.history.presentation.components.HistoryEntryItem
import com.agcoding.networkapp.history.presentation.components.MonthGroupHeader
import com.agcoding.networkapp.history.presentation.components.SwipeDeleteBackground

@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEntryDetails: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        onNavigateToEntryDetails = onNavigateToEntryDetails,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onIntent: (HistoryIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToEntryDetails: (Long) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_history)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (!uiState.isLoading) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { onIntent(HistoryIntent.UpdateSearch(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.hint_search_history)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null,
                            modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onIntent(HistoryIntent.UpdateSearch("")) }) {
                                Icon(Icons.Default.Close, contentDescription = null,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    ),
                )
                if (uiState.accounts.size > 1) {
                    AccountFilterRow(
                        accounts = uiState.accounts,
                        selectedAccountId = uiState.selectedAccountId,
                        onSelect = { onIntent(HistoryIntent.SelectAccount(it)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.groupedEntries.isEmpty() -> {
                    val hasActiveFilter = uiState.searchQuery.isNotEmpty() || uiState.selectedAccountId != null
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(
                                if (hasActiveFilter) R.string.label_no_search_results
                                else R.string.label_empty_history
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        uiState.groupedEntries.forEach { group ->
                            item(key = group.monthHeader) {
                                MonthGroupHeader(title = group.monthHeader)
                            }
                            items(items = group.entries, key = { it.id }) { entry ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            onIntent(HistoryIntent.DeleteEntry(entry.id))
                                            true
                                        } else false
                                    },
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = { SwipeDeleteBackground() },
                                ) {
                                    HistoryEntryItem(
                                        entry = entry,
                                        onClick = { onNavigateToEntryDetails(entry.id) },
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

@Composable
private fun AccountFilterRow(
    accounts: List<Account>,
    selectedAccountId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AccountChip(
            label = stringResource(R.string.filter_all),
            isSelected = selectedAccountId == null,
            colorHex = null,
            onClick = { onSelect(null) },
        )
        accounts.forEach { account ->
            AccountChip(
                label = account.name,
                isSelected = account.id == selectedAccountId,
                colorHex = account.colorHex,
                onClick = { onSelect(account.id) },
            )
        }
    }
}

@Composable
private fun AccountChip(
    label: String,
    isSelected: Boolean,
    colorHex: String?,
    onClick: () -> Unit,
) {
    val accentColor = if (colorHex != null) {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (e: Exception) { MaterialTheme.colorScheme.primary }
    } else null

    val bgColor = when {
        isSelected && accentColor != null -> accentColor
        isSelected                         -> MaterialTheme.colorScheme.onSurface
        else                               -> Color.Transparent
    }
    val borderColor = accentColor?.copy(alpha = 0.5f)
        ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val textColor = if (isSelected) MaterialTheme.colorScheme.surface
                   else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = bgColor,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
