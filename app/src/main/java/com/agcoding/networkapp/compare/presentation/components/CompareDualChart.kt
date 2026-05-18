package com.agcoding.networkapp.compare.presentation.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.agcoding.networkapp.R
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.theme.DarkBackground
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

private val PreviousColor = Color(0xFF8B7FD4)

@Composable
fun CompareDualChart(
    currentData: List<ChartPoint>,
    previousData: List<ChartPoint>,
    currentLabel: String,
    previousLabel: String,
    modifier: Modifier = Modifier
) {
    val animProgress = remember(currentData, previousData) { Animatable(0f) }
    LaunchedEffect(currentData, previousData) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.compare_chart_title),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.compare_chart_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val progress = animProgress.value
                Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 4.dp)) {
                    val w = size.width
                    val h = size.height
                    val vPad = h * 0.08f

                    fun toX(pt: ChartPoint) = pt.x * w
                    fun toY(pt: ChartPoint) = vPad + (1f - pt.y) * (h - 2 * vPad)

                    fun drawSeries(
                        points: List<ChartPoint>,
                        lineColor: Color,
                        fillColor: Color
                    ) {
                        if (points.size < 2) return
                        val visibleCount = (points.size * progress).toInt().coerceAtLeast(2)
                        val visible = points.take(visibleCount)
                        val last = visible.last()

                        val fillPath = Path()
                        visible.forEachIndexed { i, pt ->
                            val x = toX(pt); val y = toY(pt)
                            if (i == 0) { fillPath.moveTo(x, h); fillPath.lineTo(x, y) }
                            else fillPath.lineTo(x, y)
                        }
                        fillPath.lineTo(toX(last), h)
                        fillPath.close()
                        drawPath(fillPath, brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)))

                        val linePath = Path()
                        visible.forEachIndexed { i, pt ->
                            if (i == 0) linePath.moveTo(toX(pt), toY(pt))
                            else linePath.lineTo(toX(pt), toY(pt))
                        }
                        drawPath(linePath, color = lineColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                        visible.forEach { pt ->
                            drawCircle(lineColor.copy(alpha = 0.3f), 3.dp.toPx(), Offset(toX(pt), toY(pt)))
                        }
                        drawCircle(Color.White, 4.dp.toPx(), Offset(toX(last), toY(last)))
                        drawCircle(lineColor, 2.5.dp.toPx(), Offset(toX(last), toY(last)))
                    }

                    drawSeries(previousData, PreviousColor, PreviousColor.copy(alpha = 0.15f))
                    drawSeries(currentData, PositiveGreen, PositiveGreen.copy(alpha = 0.2f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(color = PreviousColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text(previousLabel, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
                Spacer(modifier = Modifier.width(20.dp))
                LegendDot(color = PositiveGreen)
                Spacer(modifier = Modifier.width(6.dp))
                Text(currentLabel, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Surface(
        modifier = Modifier.size(8.dp),
        shape = CircleShape,
        color = color
    ) {}
}

@Preview(showBackground = true)
@Composable
private fun CompareDualChartPreview() {
    val current = listOf(0.1f, 0.3f, 0.4f, 0.55f, 0.65f, 0.9f)
        .mapIndexed { i, y -> ChartPoint(i.toFloat() / 5f, y) }
    val previous = listOf(0.05f, 0.2f, 0.35f, 0.4f, 0.5f, 0.7f)
        .mapIndexed { i, y -> ChartPoint(i.toFloat() / 5f, y) }
    NetWorthTheme {
        CompareDualChart(
            currentData = current,
            previousData = previous,
            currentLabel = "2026",
            previousLabel = "2025"
        )
    }
}
