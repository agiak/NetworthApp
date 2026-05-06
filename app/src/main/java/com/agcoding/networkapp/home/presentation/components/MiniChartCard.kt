package com.agcoding.networkapp.home.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun MiniChartCard(
    chartData: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(chartData) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Trend",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            val lineColor = MaterialTheme.colorScheme.primary
            val progress = animationProgress.value
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                if (chartData.size < 2) return@Canvas
                val visibleCount = (chartData.size * progress).toInt().coerceAtLeast(2)
                val visible = chartData.take(visibleCount)
                val padding = size.height * 0.1f

                val path = Path()
                visible.forEachIndexed { index, point ->
                    val x = point.x * size.width
                    val y = padding + (1f - point.y) * (size.height - 2 * padding)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val last = visible.last()
                val lastX = last.x * size.width
                val lastY = padding + (1f - last.y) * (size.height - 2 * padding)
                drawCircle(color = lineColor, radius = 5.dp.toPx(), center = Offset(lastX, lastY))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MiniChartCardPreview() {
    NetWorthTheme {
        MiniChartCard(
            chartData = listOf(
                ChartPoint(0f, 0.3f), ChartPoint(0.2f, 0.5f),
                ChartPoint(0.4f, 0.45f), ChartPoint(0.6f, 0.7f),
                ChartPoint(0.8f, 0.8f), ChartPoint(1f, 1f)
            )
        )
    }
}
