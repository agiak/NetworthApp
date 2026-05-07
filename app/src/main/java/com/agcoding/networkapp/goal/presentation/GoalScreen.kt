package com.agcoding.networkapp.goal.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.goal.presentation.components.GoalProgressSection
import com.agcoding.networkapp.goal.presentation.components.GoalStatsCards
import com.agcoding.networkapp.goal.presentation.components.GoalStatusCard
import com.agcoding.networkapp.goal.presentation.components.GoalTimeframeSelector
import com.agcoding.networkapp.goal.presentation.components.GoalTrajectoryChart
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun GoalScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GoalContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalContent(
    uiState: GoalUiState,
    onIntent: (GoalIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(GoalIntent.ClearError)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.goal_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Input card
                item {
                    GoalInputCard(
                        targetInput = uiState.targetInput,
                        currentNetWorth = uiState.currentNetWorthFormatted,
                        onTargetChange = { onIntent(GoalIntent.UpdateTargetInput(it)) }
                    )
                }

                // Timeframe selector
                item {
                    GoalTimeframeSelector(
                        selectedTimeframe = uiState.selectedTimeframe,
                        onTimeframeSelect = { onIntent(GoalIntent.SelectTimeframe(it)) }
                    )
                }

                // Custom years input
                if (uiState.selectedTimeframe == GoalTimeframe.CUSTOM) {
                    item {
                        CustomYearsInput(
                            input = uiState.customYearsInput,
                            onChange = { onIntent(GoalIntent.UpdateCustomYears(it)) }
                        )
                    }
                }

                // Results section — animates on timeframe change
                item {
                    AnimatedContent(
                        targetState = Triple(
                            uiState.selectedTimeframe,
                            uiState.customYearsInput,
                            uiState.targetInput
                        ),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "goalResults"
                    ) { _ ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            when {
                                !uiState.isInputValid || uiState.targetInput.isBlank() -> {
                                    GoalHintCard(text = stringResource(R.string.goal_enter_target))
                                }
                                !uiState.hasHistoricalData -> {
                                    GoalHintCard(text = stringResource(R.string.goal_no_data))
                                }
                                uiState.status == GoalStatus.GOAL_REACHED -> {
                                    GoalReachedSection(
                                        progressFraction = uiState.progressFraction,
                                        progressPercent = uiState.progressPercent,
                                        currentFormatted = uiState.currentNetWorthFormatted,
                                        targetFormatted = uiState.targetFormatted
                                    )
                                }
                                else -> {
                                    GoalProgressSection(
                                        progressFraction = uiState.progressFraction,
                                        progressPercent = uiState.progressPercent,
                                        currentFormatted = uiState.currentNetWorthFormatted,
                                        targetFormatted = uiState.targetFormatted
                                    )
                                    GoalStatusCard(
                                        status = uiState.status,
                                        fasterByPercent = uiState.fasterByPercent,
                                        yearsAtCurrentPace = uiState.yearsAtCurrentPace
                                    )
                                    GoalStatsCards(
                                        requiredMonthly = uiState.requiredMonthly,
                                        requiredYearly = uiState.requiredYearly,
                                        avgMonthlyGrowth = uiState.avgMonthlyGrowth,
                                        yearsAtCurrentPace = uiState.yearsAtCurrentPace
                                    )
                                    if (uiState.chartCurrentTrajectory.size >= 2) {
                                        GoalTrajectoryChart(
                                            currentTrajectory = uiState.chartCurrentTrajectory,
                                            requiredTrajectory = uiState.chartRequiredTrajectory,
                                            endLabel = uiState.chartEndLabel
                                        )
                                        GoalChartLegend()
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun GoalInputCard(
    targetInput: String,
    currentNetWorth: String,
    onTargetChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.goal_target_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = targetInput,
                onValueChange = { raw ->
                    if (raw.all { it.isDigit() } && raw.length <= 10) onTargetChange(raw)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.goal_target_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                prefix = { Text("€", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PositiveGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            if (currentNetWorth.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.goal_current_net_worth, currentNetWorth),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CustomYearsInput(
    input: String,
    onChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.goal_custom_years_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { raw ->
                    val n = raw.toIntOrNull()
                    if (raw.isEmpty() || (n != null && n in 1..30)) onChange(raw)
                },
                modifier = Modifier.width(100.dp),
                suffix = {
                    Text(
                        text = stringResource(R.string.goal_years_suffix),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PositiveGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@Composable
private fun GoalHintCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun GoalReachedSection(
    progressFraction: Float,
    progressPercent: String,
    currentFormatted: String,
    targetFormatted: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GoalProgressSection(
            progressFraction = progressFraction,
            progressPercent = progressPercent,
            currentFormatted = currentFormatted,
            targetFormatted = targetFormatted
        )
        GoalStatusCard(
            status = GoalStatus.GOAL_REACHED,
            fasterByPercent = 0,
            yearsAtCurrentPace = ""
        )
    }
}

@Composable
private fun GoalChartLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LegendItem(color = PositiveGreen, label = stringResource(R.string.goal_chart_required))
        LegendItem(color = Color.White.copy(alpha = 0.4f), label = stringResource(R.string.goal_chart_your_pace))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            modifier = Modifier.width(16.dp).height(2.dp),
            shape = RoundedCornerShape(1.dp),
            color = color
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalContentPreview() {
    val pts = (0..24).map { i -> ChartPoint(i.toFloat() / 24f, i.toFloat() / 24f * 0.7f + 0.1f) }
    NetWorthTheme {
        GoalContent(
            uiState = GoalUiState(
                isLoading = false,
                hasHistoricalData = true,
                targetInput = "100000",
                selectedTimeframe = GoalTimeframe.THREE_YEARS,
                currentNetWorthFormatted = "€42,000",
                currentNetWorthRaw = 42000.0,
                targetFormatted = "€100,000",
                progressFraction = 0.42f,
                progressPercent = "42%",
                status = GoalStatus.SLIGHTLY_BEHIND,
                requiredMonthly = "€1,620",
                requiredYearly = "€19,444",
                avgMonthlyGrowth = "+€683",
                fasterByPercent = 137,
                yearsAtCurrentPace = "7Y 4M",
                isInputValid = true,
                chartCurrentTrajectory = pts,
                chartRequiredTrajectory = pts.map { it.copy(y = (it.y + 0.2f).coerceAtMost(1f)) },
                chartEndLabel = "3Y"
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}
