package com.agcoding.networkapp.analytics.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
    viewModel: AllMonthsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AllMonthsContent(uiState = uiState, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllMonthsContent(
    uiState: AllMonthsUiState,
    onNavigateBack: () -> Unit
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
            onNavigateBack = {}
        )
    }
}
