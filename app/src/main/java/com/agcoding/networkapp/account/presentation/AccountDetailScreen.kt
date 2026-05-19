package com.agcoding.networkapp.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.analytics.presentation.components.AnalyticsChartCard
import com.agcoding.networkapp.analytics.presentation.components.AnalyticsSummaryCard
import com.agcoding.networkapp.home.presentation.components.EntryHistoryItem
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColorScheme.current

    val accentColor = if (uiState.accountColorHex.isNotEmpty()) {
        try { Color(android.graphics.Color.parseColor(uiState.accountColorHex)) }
        catch (e: Exception) { colors.actionPrimary }
    } else colors.actionPrimary

    val changeColor = if (uiState.isPositiveChange) colors.statusSuccess else colors.statusError
    val growthColor = if (uiState.totalGrowthPositive) colors.statusSuccess else colors.statusError

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = uiState.accountName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(uiState.accountName, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            !uiState.hasData -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.label_empty_history),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Current balance hero
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = stringResource(R.string.label_current_balance).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = uiState.currentBalance,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                )
                                if (uiState.entryCount > 1) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = uiState.change,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = changeColor,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    // Stats row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AnalyticsSummaryCard(
                                label = stringResource(R.string.analytics_total_growth),
                                value = uiState.totalGrowth,
                                subLabel = uiState.totalGrowthPercent,
                                valueColor = growthColor,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            AnalyticsSummaryCard(
                                label = stringResource(R.string.analytics_avg_month),
                                value = uiState.avgPerMonth,
                                subLabel = stringResource(R.string.label_entries_count, uiState.entryCount),
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }

                    // Chart
                    if (uiState.chartData.size >= 2) {
                        item {
                            AnalyticsChartCard(
                                chartData   = uiState.chartData,
                                startLabel  = "",
                                midLabel    = "",
                                endLabel    = "",
                            )
                        }
                    }

                    // Recent entries
                    if (uiState.recentEntries.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.section_recent_entries),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        items(uiState.recentEntries.size) { index ->
                            EntryHistoryItem(entry = uiState.recentEntries[index])
                        }
                    }
                }
            }
        }
    }
}
