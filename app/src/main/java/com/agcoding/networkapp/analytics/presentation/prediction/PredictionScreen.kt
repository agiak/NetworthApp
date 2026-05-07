package com.agcoding.networkapp.analytics.presentation.prediction

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.analytics.presentation.prediction.components.PredictionExpectedCard
import com.agcoding.networkapp.analytics.presentation.prediction.components.PredictionForecastChart
import com.agcoding.networkapp.analytics.presentation.prediction.components.PredictionInfoCard
import com.agcoding.networkapp.analytics.presentation.prediction.components.PredictionRangeSelector
import com.agcoding.networkapp.analytics.presentation.prediction.components.PredictionScenarioCards
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun PredictionScreen(
    onNavigateBack: () -> Unit,
    viewModel: PredictionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PredictionContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PredictionContent(
    uiState: PredictionUiState,
    onIntent: (PredictionIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(PredictionIntent.ClearError)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.prediction_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
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
                ) { CircularProgressIndicator() }
            }
            !uiState.hasData -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.prediction_no_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    item {
                        PredictionHeader(avgMonthlyGrowth = uiState.avgMonthlyGrowth)
                    }

                    // Range selector
                    item {
                        PredictionRangeSelector(
                            selectedRange = uiState.selectedRange,
                            onRangeSelect = { onIntent(PredictionIntent.SelectRange(it)) }
                        )
                    }

                    // Chart + values animate on range change
                    item {
                        AnimatedContent(
                            targetState = uiState.selectedRange,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "predictionContent"
                        ) { _ ->
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PredictionForecastChart(
                                    expectedPoints = uiState.chartExpected,
                                    minimumPoints = uiState.chartMinimum,
                                    maximumPoints = uiState.chartMaximum,
                                    expectedLabel = uiState.chartExpectedLabel,
                                    minimumLabel = uiState.chartMinimumLabel,
                                    maximumLabel = uiState.chartMaximumLabel,
                                    midLabel = uiState.chartMidLabel,
                                    endLabel = uiState.chartEndLabel
                                )

                                PredictionExpectedCard(
                                    expectedValue = uiState.expectedValue,
                                    years = uiState.selectedRange.years
                                )

                                PredictionScenarioCards(
                                    minimumValue = uiState.minimumValue,
                                    maximumValue = uiState.maximumValue,
                                    conservativeMonthlyRate = uiState.conservativeMonthlyRate,
                                    optimisticMonthlyRate = uiState.optimisticMonthlyRate
                                )

                                PredictionInfoCard(
                                    avgMonthlyGrowth = uiState.avgMonthlyGrowth,
                                    conservativeMonthlyRate = uiState.conservativeMonthlyRate,
                                    optimisticMonthlyRate = uiState.optimisticMonthlyRate
                                )
                            }
                        }
                    }

                    // Disclaimer
                    item {
                        Text(
                            text = stringResource(R.string.prediction_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PredictionHeader(avgMonthlyGrowth: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.prediction_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.prediction_avg_growth, avgMonthlyGrowth),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PredictionContentPreview() {
    val pts = (0..24).map { i ->
        ChartPoint(x = i.toFloat() / 24f, y = 0.2f + i * 0.03f)
    }
    NetWorthTheme {
        PredictionContent(
            uiState = PredictionUiState(
                isLoading = false,
                hasData = true,
                selectedRange = PredictionRange.FIVE_YEARS,
                expectedValue = "€72,400",
                minimumValue = "€45,200",
                maximumValue = "€99,600",
                avgMonthlyGrowth = "+€683",
                chartExpected = pts,
                chartMinimum = pts.map { it.copy(y = (it.y - 0.15f).coerceAtLeast(0f)) },
                chartMaximum = pts.map { it.copy(y = (it.y + 0.1f).coerceAtMost(1f)) },
                chartExpectedLabel = "€72K",
                chartMinimumLabel = "€45K",
                chartMaximumLabel = "€99K",
                conservativeMonthlyRate = "+€350",
                optimisticMonthlyRate = "+€950",
                chartMidLabel = "2.5Y",
                chartEndLabel = "5Y"
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}
