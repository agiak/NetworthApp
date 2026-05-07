package com.agcoding.networkapp.analytics.presentation.prediction.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.theme.DarkBackground
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

private val CHART_TOTAL_HEIGHT = 230.dp
private val X_AXIS_HEIGHT = 32.dp
private const val V_PAD_FRAC = 0.08f
private val LABEL_MIN_GAP = 14.dp

@Composable
fun PredictionForecastChart(
    expectedPoints: List<ChartPoint>,
    minimumPoints: List<ChartPoint>,
    maximumPoints: List<ChartPoint>,
    expectedLabel: String,
    minimumLabel: String,
    maximumLabel: String,
    midLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier
) {
    val animProgress = remember(expectedPoints) { Animatable(0f) }
    LaunchedEffect(expectedPoints) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }
    val progress = animProgress.value

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBackground)
    ) {
        // BoxWithConstraintsScope extends BoxScope → .align() is available here
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_TOTAL_HEIGHT)
        ) {
            if (expectedPoints.size >= 2) {

                // Line chart — right-padded to make room for end labels
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = X_AXIS_HEIGHT, end = 52.dp)
                ) {
                    val visibleCount = (expectedPoints.size * progress).toInt().coerceAtLeast(2)
                    val visExp = expectedPoints.take(visibleCount)
                    val visMin = minimumPoints.take(visibleCount)
                    val visMax = maximumPoints.take(visibleCount)
                    val w = size.width
                    val h = size.height
                    val vPadPx = h * V_PAD_FRAC

                    fun toX(pt: ChartPoint) = pt.x * w
                    fun toY(pt: ChartPoint) = vPadPx + (1f - pt.y) * (h - 2 * vPadPx)

                    // Confidence band fill
                    val bandPath = Path()
                    visMax.forEachIndexed { i, pt ->
                        if (i == 0) bandPath.moveTo(toX(pt), toY(pt))
                        else bandPath.lineTo(toX(pt), toY(pt))
                    }
                    visMin.reversed().forEach { pt -> bandPath.lineTo(toX(pt), toY(pt)) }
                    bandPath.close()
                    drawPath(bandPath, color = PositiveGreen.copy(alpha = 0.10f))

                    // Max line
                    val maxPath = Path()
                    visMax.forEachIndexed { i, pt ->
                        if (i == 0) maxPath.moveTo(toX(pt), toY(pt))
                        else maxPath.lineTo(toX(pt), toY(pt))
                    }
                    drawPath(maxPath, color = Color.White.copy(alpha = 0.25f),
                        style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))

                    // Min line
                    val minPath = Path()
                    visMin.forEachIndexed { i, pt ->
                        if (i == 0) minPath.moveTo(toX(pt), toY(pt))
                        else minPath.lineTo(toX(pt), toY(pt))
                    }
                    drawPath(minPath, color = Color.White.copy(alpha = 0.25f),
                        style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))

                    // Expected gradient fill
                    val lastExp = visExp.last()
                    val fillPath = Path()
                    visExp.forEachIndexed { i, pt ->
                        val x = toX(pt); val y = toY(pt)
                        if (i == 0) { fillPath.moveTo(x, h); fillPath.lineTo(x, y) }
                        else fillPath.lineTo(x, y)
                    }
                    fillPath.lineTo(toX(lastExp), h)
                    fillPath.close()
                    drawPath(
                        fillPath,
                        brush = Brush.verticalGradient(
                            listOf(PositiveGreen.copy(alpha = 0.30f), Color.Transparent)
                        )
                    )

                    // Expected line
                    val expPath = Path()
                    visExp.forEachIndexed { i, pt ->
                        if (i == 0) expPath.moveTo(toX(pt), toY(pt))
                        else expPath.lineTo(toX(pt), toY(pt))
                    }
                    drawPath(expPath, color = PositiveGreen,
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                    // End dot
                    drawCircle(Color.White, 5.dp.toPx(), Offset(toX(lastExp), toY(lastExp)))
                    drawCircle(PositiveGreen, 3.5.dp.toPx(), Offset(toX(lastExp), toY(lastExp)))
                }

                // End-value labels (appear after animation completes)
                if (progress >= 0.98f && expectedLabel.isNotEmpty()) {
                    val drawArea: Dp = CHART_TOTAL_HEIGHT - X_AXIS_HEIGHT
                    val vPad: Dp = drawArea * V_PAD_FRAC
                    val usable: Dp = drawArea * (1f - 2 * V_PAD_FRAC)
                    val halfLabel = 5.dp

                    fun yToTop(y: Float): Dp = vPad + usable * (1f - y) - halfLabel

                    val rawExpTop = yToTop(expectedPoints.last().y)
                    val rawMaxTop = yToTop(maximumPoints.last().y)
                    val rawMinTop = yToTop(minimumPoints.last().y)

                    // Prevent overlap
                    val maxTop = minOf(rawMaxTop, rawExpTop - LABEL_MIN_GAP)
                    val expTop = rawExpTop
                    val minTop = maxOf(rawMinTop, rawExpTop + LABEL_MIN_GAP)

                    Text(
                        text = maximumLabel,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = maxTop)
                            .padding(end = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PositiveGreen.copy(alpha = 0.9f)
                    )
                    Text(
                        text = expectedLabel,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = expTop)
                            .padding(end = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = minimumLabel,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = minTop)
                            .padding(end = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }
            }

            // X-axis labels
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.prediction_chart_now),
                    style = MaterialTheme.typography.labelSmall, color = Color.Gray)
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
private fun PredictionForecastChartPreview() {
    val pts = listOf(
        ChartPoint(0f, 0.3f), ChartPoint(0.25f, 0.45f),
        ChartPoint(0.5f, 0.6f), ChartPoint(0.75f, 0.75f), ChartPoint(1f, 0.9f)
    )
    NetWorthTheme {
        PredictionForecastChart(
            expectedPoints = pts,
            minimumPoints = pts.map { it.copy(y = (it.y - 0.15f).coerceAtLeast(0f)) },
            maximumPoints = pts.map { it.copy(y = (it.y + 0.1f).coerceAtMost(1f)) },
            expectedLabel = "€72K", minimumLabel = "€45K", maximumLabel = "€99K",
            midLabel = "2.5Y", endLabel = "5Y"
        )
    }
}
