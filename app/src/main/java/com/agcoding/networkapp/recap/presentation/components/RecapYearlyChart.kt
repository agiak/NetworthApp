package com.agcoding.networkapp.recap.presentation.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.theme.DarkBackground
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun RecapYearlyChart(
    chartData: List<ChartPoint>,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier
) {
    val animProgress = remember(chartData) { Animatable(0f) }
    LaunchedEffect(chartData) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBackground)
    ) {
        Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
            Text(
                text = stringResource(R.string.recap_chart_title),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                if (chartData.size >= 2) {
                    val progress = animProgress.value
                    Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 28.dp)) {
                        val visibleCount = (chartData.size * progress).toInt().coerceAtLeast(2)
                        val visible = chartData.take(visibleCount)
                        val w = size.width
                        val h = size.height
                        val vPad = h * 0.08f

                        fun toX(pt: ChartPoint) = pt.x * w
                        fun toY(pt: ChartPoint) = vPad + (1f - pt.y) * (h - 2 * vPad)

                        val lastPt = visible.last()

                        // Fill
                        val fillPath = Path()
                        visible.forEachIndexed { i, pt ->
                            val x = toX(pt); val y = toY(pt)
                            if (i == 0) { fillPath.moveTo(x, h); fillPath.lineTo(x, y) }
                            else fillPath.lineTo(x, y)
                        }
                        fillPath.lineTo(toX(lastPt), h)
                        fillPath.close()
                        drawPath(fillPath, brush = Brush.verticalGradient(listOf(PositiveGreen.copy(0.25f), Color.Transparent)))

                        // Line
                        val linePath = Path()
                        visible.forEachIndexed { i, pt ->
                            if (i == 0) linePath.moveTo(toX(pt), toY(pt))
                            else linePath.lineTo(toX(pt), toY(pt))
                        }
                        drawPath(linePath, color = PositiveGreen, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                        // Dots on each data point
                        visible.forEach { pt ->
                            drawCircle(PositiveGreen.copy(alpha = 0.4f), 4.dp.toPx(), Offset(toX(pt), toY(pt)))
                        }
                        drawCircle(Color.White, 5.dp.toPx(), Offset(toX(lastPt), toY(lastPt)))
                        drawCircle(PositiveGreen, 3.5.dp.toPx(), Offset(toX(lastPt), toY(lastPt)))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(startLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(endLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecapYearlyChartPreview() {
    val pts = listOf(0.2f, 0.35f, 0.3f, 0.5f, 0.6f, 0.55f, 0.7f, 0.65f, 0.8f, 0.75f, 0.85f, 0.9f)
        .mapIndexed { i, y -> ChartPoint(i.toFloat() / 11f, y) }
    NetWorthTheme {
        RecapYearlyChart(chartData = pts, startLabel = "Jan", endLabel = "Dec")
    }
}
