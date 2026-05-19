package com.agcoding.networkapp.history.presentation

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme

private val ITEM_HORIZONTAL_PADDING = 16.dp

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
                        .padding(horizontal = ITEM_HORIZONTAL_PADDING, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.hint_search_history)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onIntent(HistoryIntent.UpdateSearch("")) }) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    ),
                )
                if (uiState.accounts.size > 1) {
                    AccountFilterRow(
                        accounts = uiState.accounts,
                        selectedAccountId = uiState.selectedAccountId,
                        onSelect = { onIntent(HistoryIntent.SelectAccount(it)) },
                        modifier = Modifier.padding(horizontal = ITEM_HORIZONTAL_PADDING, vertical = 4.dp),
                    )
                }
                LabeledFilterRow(label = stringResource(R.string.label_filter_date)) {
                    DateFilterRow(
                        selected = uiState.dateFilter,
                        onSelect = { onIntent(HistoryIntent.SelectDateFilter(it)) },
                    )
                }
                LabeledFilterRow(label = stringResource(R.string.label_filter_sort)) {
                    SortFilterRow(
                        selected = uiState.sortOrder,
                        onSelect = { onIntent(HistoryIntent.SelectSort(it)) },
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
                    val hasActiveFilter = uiState.searchQuery.isNotEmpty()
                        || uiState.selectedAccountId != null
                        || uiState.dateFilter != HistoryDateFilter.ALL
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
                        contentPadding = PaddingValues(horizontal = ITEM_HORIZONTAL_PADDING, vertical = 8.dp),
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
                                            onIntent(HistoryIntent.RequestDeleteConfirmation(entry.id))
                                        }
                                        false
                                    },
                                )
                                val errorColor = LocalAppColorScheme.current.statusError
                                val bgColor by animateColorAsState(
                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                        errorColor else Color.Transparent,
                                    label = "swipe_bg",
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    modifier = Modifier.fillMaxWidth(),
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        SwipeDeleteBackground(tintColor = bgColor)
                                    },
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

    if (uiState.pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { onIntent(HistoryIntent.DismissDeleteConfirmation) },
            title = { Text(stringResource(R.string.delete_entry_confirm_title)) },
            text = { Text(stringResource(R.string.delete_entry_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(HistoryIntent.DeleteEntry(uiState.pendingDeleteId)) }) {
                    Text(
                        text = stringResource(R.string.btn_delete_entry),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(HistoryIntent.DismissDeleteConfirmation) }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }
}

@Composable
private fun LabeledFilterRow(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(horizontal = ITEM_HORIZONTAL_PADDING, vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        content()
    }
}

@Composable
private fun DateFilterRow(
    selected: HistoryDateFilter,
    onSelect: (HistoryDateFilter) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val options = listOf(
            HistoryDateFilter.ALL to stringResource(R.string.filter_all),
            HistoryDateFilter.ONE_MONTH to stringResource(R.string.history_date_filter_1m),
            HistoryDateFilter.THREE_MONTHS to stringResource(R.string.history_date_filter_3m),
            HistoryDateFilter.SIX_MONTHS to stringResource(R.string.history_date_filter_6m),
            HistoryDateFilter.ONE_YEAR to stringResource(R.string.history_date_filter_1y),
        )
        options.forEach { (filter, label) ->
            FilterChip(label = label, isSelected = filter == selected, onClick = { onSelect(filter) })
        }
    }
}

@Composable
private fun SortFilterRow(
    selected: HistorySortOrder,
    onSelect: (HistorySortOrder) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val options = listOf(
            HistorySortOrder.NEWEST_FIRST to stringResource(R.string.sort_newest_first),
            HistorySortOrder.OLDEST_FIRST to stringResource(R.string.sort_oldest_first),
        )
        options.forEach { (order, label) ->
            FilterChip(label = label, isSelected = order == selected, onClick = { onSelect(order) })
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) else null,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
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
        isSelected -> MaterialTheme.colorScheme.onSurface
        else -> Color.Transparent
    }
    val borderColor = accentColor?.copy(alpha = 0.5f)
        ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = if (!isSelected) BorderStroke(1.dp, borderColor) else null,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
