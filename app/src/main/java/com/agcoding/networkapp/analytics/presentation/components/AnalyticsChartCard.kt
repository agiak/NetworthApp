package com.agcoding.networkapp.analytics.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun AnalyticsChartCard(
    chartData: List<ChartPoint>,
    startLabel: String,
    midLabel: String,
    endLabel: String,
    headerLabel: String = "",
    currentNetWorth: String = "",
    totalGrowth: String = "",
    totalGrowthPercent: String = "",
    totalGrowthPositive: Boolean = true,
    topLabel: String = "",
    bottomLabel: String = "",
    modifier: Modifier = Modifier
) {
    val animationProgress = remember(chartData) { Animatable(0f) }
    LaunchedEffect(chartData) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }

    val lineColor   = Color(0xFF1A1A2E)
    val fillColor   = PositiveGreen.copy(alpha = 0.15f)
    val dotColor    = Color(0xFF1A1A2E)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        color    = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)) {

            // Header: label
            if (headerLabel.isNotEmpty()) {
                Text(
                    text = headerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(4.dp))
            }

            // Current net worth
            if (currentNetWorth.isNotEmpty()) {
                Text(
                    text = currentNetWorth,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
            }

            // Change row
            if (totalGrowth.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = totalGrowth,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (totalGrowthPositive) PositiveGreen else MaterialTheme.colorScheme.error,
                    )
                    if (totalGrowthPercent.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$totalGrowthPercent ${stringResource(R.string.analytics_this_period)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (chartData.size >= 2) {
                    val progress = animationProgress.value
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 28.dp)
                    ) {
                        val visibleCount = (chartData.size * progress).toInt().coerceAtLeast(2)
                        val visible = chartData.take(visibleCount)
                        val w = size.width
                        val h = size.height
                        val vPad = h * 0.05f

                        val linePath = Path()
                        val fillPath = Path()

                        visible.forEachIndexed { i, pt ->
                            val x = pt.x * w
                            val y = vPad + (1f - pt.y) * (h - 2 * vPad)
                            if (i == 0) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, h)
                                fillPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                            if (i == visible.size - 1) {
                                fillPath.lineTo(x, h)
                                fillPath.close()
                            }
                        }

                        drawPath(
                            path  = fillPath,
                            brush = Brush.verticalGradient(colors = listOf(fillColor, Color.Transparent))
                        )
                        drawPath(
                            path  = linePath,
                            color = lineColor,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Dots on all visible data points
                        visible.forEach { pt ->
                            val x = pt.x * w
                            val y = vPad + (1f - pt.y) * (h - 2 * vPad)
                            drawCircle(color = dotColor, radius = 3.dp.toPx(), center = Offset(x, y))
                        }

                        // Larger white-bordered dot at the last visible point
                        val last  = visible.last()
                        val lastX = last.x * w
                        val lastY = vPad + (1f - last.y) * (h - 2 * vPad)
                        drawCircle(color = Color.White,    radius = 5.dp.toPx(), center = Offset(lastX, lastY))
                        drawCircle(color = dotColor,       radius = 3.5.dp.toPx(), center = Offset(lastX, lastY))
                    }
                }

                // X-axis labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (midLabel.isNotEmpty()) {
                        Text(midLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(endLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsChartCardPreview() {
    NetWorthTheme {
        AnalyticsChartCard(
            chartData = listOf(
                ChartPoint(0f, 0.2f), ChartPoint(0.14f, 0.25f), ChartPoint(0.29f, 0.22f),
                ChartPoint(0.43f, 0.35f), ChartPoint(0.57f, 0.42f), ChartPoint(0.71f, 0.55f),
                ChartPoint(0.86f, 0.7f),  ChartPoint(1f, 0.9f)
            ),
            startLabel = "May '25",
            midLabel = "Nov '25",
            endLabel = "May '26",
            headerLabel = "TOTAL · 12 MONTHS",
            currentNetWorth = "€18,200",
            totalGrowth = "+€8,200",
            totalGrowthPercent = "+82.0%",
            totalGrowthPositive = true,
        )
    }
}
