package com.agcoding.networkapp.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.agcoding.networkapp.home.presentation.components.AddEntryBottomSheet
import com.agcoding.networkapp.home.presentation.components.GoalTeaserCard
import com.agcoding.networkapp.home.presentation.components.InsightsCard
import com.agcoding.networkapp.home.presentation.components.NetWorthHeroCard
import com.agcoding.networkapp.home.presentation.components.RecentEntriesSection
import com.agcoding.networkapp.home.presentation.components.StatCard
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.home.presentation.model.InsightData
import com.agcoding.networkapp.shared.ui.components.ProjectionCard
import com.agcoding.networkapp.shared.ui.model.EntryUiModel
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToProfileEdit: () -> Unit,
    onNavigateToEntryDetails: (Long) -> Unit,
    onNavigateToGoal: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToCreateAccount: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToProfileEdit = onNavigateToProfileEdit,
        onNavigateToEntryDetails = onNavigateToEntryDetails,
        onNavigateToGoal = onNavigateToGoal,
        onNavigateToAccounts = onNavigateToAccounts,
        onNavigateToCreateAccount = onNavigateToCreateAccount,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfileEdit: () -> Unit,
    onNavigateToEntryDetails: (Long) -> Unit,
    onNavigateToGoal: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToCreateAccount: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(HomeIntent.ClearError)
        }
    }

    val hasData = !uiState.isLoading && uiState.chartData.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (hasData) {
                Button(
                    onClick = { onIntent(HomeIntent.ShowAddEntrySheet) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(56.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.label_add_entry), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            HomeHeader(name = uiState.userName, initial = uiState.userInitial, onProfileClick = onNavigateToProfileEdit)
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                !hasData -> {
                    HomeEmptyState(
                        onAddEntry = { onIntent(HomeIntent.ShowAddEntrySheet) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item {
                            NetWorthHeroCard(
                                netWorth = uiState.currentNetWorth,
                                change = uiState.changeThisMonth,
                                percentage = uiState.changePercentage,
                                isPositive = uiState.isPositiveChange,
                                chartData = uiState.chartData,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        item {
                            AccountsCard(
                                breakdown = uiState.accountBreakdown,
                                onClick = onNavigateToAccounts,
                                onAddAccount = onNavigateToCreateAccount,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        if (uiState.hasGoal) {
                            item {
                                GoalTeaserCard(
                                    currentNetWorthRaw = uiState.currentNetWorthRaw,
                                    targetAmountRaw = uiState.targetAmountRaw,
                                    targetAmountFormatted = uiState.targetAmount,
                                    onClick = onNavigateToGoal,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard(
                                    title = stringResource(R.string.stat_ytd_growth),
                                    value = uiState.ytdGrowth,
                                    subValue = uiState.ytdPercentage,
                                    isPositive = true,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                                StatCard(
                                    title = stringResource(R.string.stat_avg_month),
                                    value = uiState.avgPerMonth,
                                    subValue = stringResource(R.string.stat_last_12_mo),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                                StatCard(
                                    title = stringResource(R.string.stat_streak),
                                    value = "${uiState.streakMonths}",
                                    subValue = stringResource(R.string.stat_months_up),
                                    icon = "🔥",
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                        if (uiState.showProjection) {
                            item {
                                ProjectionCard(
                                    projectedNetWorth = uiState.projectedNetWorth,
                                    projectedNetWorthDate = uiState.projectedNetWorthDate,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                        if (uiState.insights.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = stringResource(R.string.label_insights), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        Text(text = stringResource(R.string.label_new_count, uiState.insights.size), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    uiState.insights.forEach { insight ->
                                        InsightsCard(insight = insight, currencySymbol = uiState.currencySymbol)
                                    }
                                }
                            }
                        }
                        if (uiState.recentEntries.isNotEmpty()) {
                            item {
                                RecentEntriesSection(
                                    entries = uiState.recentEntries,
                                    onShowAll = onNavigateToHistory,
                                    onEntryClick = onNavigateToEntryDetails,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isAddEntrySheetVisible) {
            AddEntryBottomSheet(
                entryInput       = uiState.entryInput,
                selectedDate     = uiState.selectedDate,
                isSaving         = uiState.isSaving,
                entrySaved       = uiState.entrySaved,
                currencySymbol   = uiState.currencySymbol,
                noteInput        = uiState.noteInput,
                accounts         = uiState.accounts,
                selectedAccountId = uiState.selectedAccountId,
                onValueChange    = { onIntent(HomeIntent.UpdateEntryInput(it)) },
                onDateChange     = { onIntent(HomeIntent.UpdateEntryDate(it)) },
                onNoteChange     = { onIntent(HomeIntent.UpdateEntryNote(it)) },
                onAccountSelected = { onIntent(HomeIntent.SelectAccount(it)) },
                onSave           = { onIntent(HomeIntent.SaveEntry) },
                onDismiss        = { onIntent(HomeIntent.HideAddEntrySheet) }
            )
        }
    }
}

@Composable
private fun HomeEmptyState(
    onAddEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "📈", fontSize = 36.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.label_empty_home_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.label_empty_home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAddEntry,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
            ),
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.btn_add_first_snapshot),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AccountsCard(
    breakdown: List<AccountBreakdownUiItem>,
    onClick: () -> Unit,
    onAddAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.label_accounts),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                // Quick-add account button
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onAddAccount),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.btn_create_account),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            if (breakdown.size > 1) {
                Spacer(Modifier.height(12.dp))

                // Stacked allocation bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                ) {
                    breakdown.forEach { item ->
                        val color = try { Color(android.graphics.Color.parseColor(item.colorHex)) }
                                    catch (e: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier
                                .weight(item.percentage.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(color),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Per-account rows
                breakdown.forEach { item ->
                    val color = try { Color(android.graphics.Color.parseColor(item.colorHex)) }
                                catch (e: Exception) { Color.Gray }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(color),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = item.formattedBalance,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = " · ${(item.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (breakdown.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = breakdown.first().formattedBalance,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(name: String, initial: String, onProfileClick: () -> Unit) {
    val currentDate = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(text = currentDate, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(text = stringResource(R.string.greeting_hi, name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = "👋", fontSize = 24.sp)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    NetWorthTheme {
        HomeContent(
            uiState = HomeUiState(
                isLoading = false,
                userName = "Maria",
                userInitial = "M",
                targetAmount = "€100,000",
                currentNetWorth = "€18,200",
                changeThisMonth = "€850",
                changePercentage = "+ 4.9 %",
                isPositiveChange = true,
                chartData = listOf(
                    ChartPoint(0f, 0.2f), ChartPoint(0.2f, 0.3f),
                    ChartPoint(0.4f, 0.25f), ChartPoint(0.6f, 0.5f),
                    ChartPoint(0.8f, 0.6f), ChartPoint(1f, 0.8f)
                ),
                insights = listOf(InsightData.GrowthStreak(streakMonths = 11)),
                recentEntries = listOf(
                    EntryUiModel(1L, "20 Mar 2026", "€16,500"),
                    EntryUiModel(2L, "15 Feb 2026", "€15,800"),
                    EntryUiModel(3L, "15 Jan 2026", "€15,100")
                ),
                selectedDate = LocalDate.now()
            ),
            onIntent = {},
            onNavigateToHistory = {},
            onNavigateToProfileEdit = {},
            onNavigateToEntryDetails = {},
            onNavigateToGoal = {},
            onNavigateToAccounts = {},
        )
    }
}
