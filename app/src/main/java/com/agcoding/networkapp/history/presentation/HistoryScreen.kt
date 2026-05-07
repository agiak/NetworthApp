package com.agcoding.networkapp.history.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.history.presentation.components.HistoryEntryItem
import com.agcoding.networkapp.history.presentation.components.MonthGroupHeader
import com.agcoding.networkapp.shared.ui.model.EntryUiModel
import com.agcoding.networkapp.shared.ui.model.GroupedEntries
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEntryDetails: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(uiState = uiState, onNavigateBack = onNavigateBack, onNavigateToEntryDetails = onNavigateToEntryDetails)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onNavigateBack: () -> Unit,
    onNavigateToEntryDetails: (Long) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_history)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.groupedEntries.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.label_empty_history),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.groupedEntries.forEach { group ->
                        item { MonthGroupHeader(title = group.monthHeader) }
                        items(items = group.entries, key = { it.id }) { entry ->
                            HistoryEntryItem(entry = entry, onClick = { onNavigateToEntryDetails(entry.id) })
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryContentPreview() {
    NetWorthTheme {
        HistoryContent(
            uiState = HistoryUiState(
                isLoading = false,
                groupedEntries = listOf(
                    GroupedEntries(
                        monthHeader = "March 2026",
                        entries = listOf(
                            EntryUiModel(1L, "20 Mar", "€16,500"),
                            EntryUiModel(2L, "5 Mar", "€16,200")
                        )
                    ),
                    GroupedEntries(
                        monthHeader = "February 2026",
                        entries = listOf(EntryUiModel(3L, "15 Feb", "€15,800"))
                    )
                )
            ),
            onNavigateBack = {},
            onNavigateToEntryDetails = {}
        )
    }
}
