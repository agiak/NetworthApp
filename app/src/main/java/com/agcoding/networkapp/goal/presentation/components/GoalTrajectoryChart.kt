package com.agcoding.networkapp.goal.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.theme.DarkBackground
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun GoalTrajectoryChart(
    currentTrajectory: List<ChartPoint>,
    requiredTrajectory: List<ChartPoint>,
    endLabel: String,
    modifier: Modifier = Modifier
) {
    val animProgress = remember(currentTrajectory) { Animatable(0f) }
    LaunchedEffect(currentTrajectory) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBackground)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            if (currentTrajectory.size >= 2) {
                val progress = animProgress.value
                Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp)) {
                    val visCount = (currentTrajectory.size * progress).toInt().coerceAtLeast(2)
                    val visCurrent = currentTrajectory.take(visCount)
                    val visRequired = requiredTrajectory.take(visCount)
                    val w = size.width
                    val h = size.height
                    val vPad = h * 0.08f

                    fun toX(pt: ChartPoint) = pt.x * w
                    fun toY(pt: ChartPoint) = vPad + (1f - pt.y) * (h - 2 * vPad)

                    // Required fill
                    val reqFill = Path()
                    visRequired.forEachIndexed { i, pt ->
                        val x = toX(pt); val y = toY(pt)
                        if (i == 0) { reqFill.moveTo(x, h); reqFill.lineTo(x, y) }
                        else reqFill.lineTo(x, y)
                    }
                    val lastReq = visRequired.last()
                    reqFill.lineTo(toX(lastReq), h); reqFill.close()
                    drawPath(reqFill, brush = Brush.verticalGradient(listOf(PositiveGreen.copy(alpha = 0.20f), Color.Transparent)))

                    // Required line (solid green)
                    val reqPath = Path()
                    visRequired.forEachIndexed { i, pt ->
                        if (i == 0) reqPath.moveTo(toX(pt), toY(pt))
                        else reqPath.lineTo(toX(pt), toY(pt))
                    }
                    drawPath(reqPath, color = PositiveGreen, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                    // Current trajectory line (lighter, muted)
                    val curPath = Path()
                    visCurrent.forEachIndexed { i, pt ->
                        if (i == 0) curPath.moveTo(toX(pt), toY(pt))
                        else curPath.lineTo(toX(pt), toY(pt))
                    }
                    drawPath(curPath, color = Color.White.copy(alpha = 0.35f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

                    // End dot on required
                    drawCircle(Color.White, 5.dp.toPx(), Offset(toX(lastReq), toY(lastReq)))
                    drawCircle(PositiveGreen, 3.5.dp.toPx(), Offset(toX(lastReq), toY(lastReq)))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.goal_chart_now), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(endLabel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalTrajectoryChartPreview() {
    val pts = (0..24).map { i -> ChartPoint(i.toFloat() / 24f, i.toFloat() / 24f * 0.8f) }
    NetWorthTheme {
        GoalTrajectoryChart(
            currentTrajectory = pts,
            requiredTrajectory = pts.map { it.copy(y = (it.y + 0.2f).coerceAtMost(1f)) },
            endLabel = "3Y"
        )
    }
}
