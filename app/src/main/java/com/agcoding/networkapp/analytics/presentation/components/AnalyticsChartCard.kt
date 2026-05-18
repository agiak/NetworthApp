package com.agcoding.networkapp.analytics.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.theme.DarkBackground
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun AnalyticsChartCard(
    chartData: List<ChartPoint>,
    startLabel: String,
    midLabel: String,
    endLabel: String,
    topLabel: String = "",
    bottomLabel: String = "",
    modifier: Modifier = Modifier
) {
    val animationProgress = remember(chartData) { Animatable(0f) }
    LaunchedEffect(chartData) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            if (chartData.size >= 2) {
                val progress = animationProgress.value
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp)
                ) {
                    val visibleCount = (chartData.size * progress).toInt().coerceAtLeast(2)
                    val visible = chartData.take(visibleCount)
                    val w = size.width
                    val h = size.height
                    val vPad = h * 0.08f

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
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(PositiveGreen.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = linePath,
                        color = PositiveGreen,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    val last = visible.last()
                    val lastX = last.x * w
                    val lastY = vPad + (1f - last.y) * (h - 2 * vPad)
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                    drawCircle(
                        color = PositiveGreen,
                        radius = 3.5.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                }
            }

            // Y-axis labels (highest at top, lowest at bottom)
            if (topLabel.isNotEmpty() || bottomLabel.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp, top = 12.dp, bottom = 38.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = topLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    )
                    Text(
                        text = bottomLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                    )
                }
            }

            // X-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(startLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                if (midLabel.isNotEmpty()) {
                    Text(midLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Text(endLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
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
                ChartPoint(0f, 0.2f), ChartPoint(0.2f, 0.35f),
                ChartPoint(0.4f, 0.3f), ChartPoint(0.6f, 0.55f),
                ChartPoint(0.8f, 0.7f), ChartPoint(1f, 0.9f)
            ),
            startLabel = "May '25",
            midLabel = "Nov '25",
            endLabel = "May '26"
        )
    }
}
