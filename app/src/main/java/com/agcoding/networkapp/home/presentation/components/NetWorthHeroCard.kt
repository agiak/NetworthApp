package com.agcoding.networkapp.home.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.shared.ui.theme.DarkBackground
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun NetWorthHeroCard(
    netWorth: String,
    change: String,
    percentage: String,
    isPositive: Boolean,
    chartData: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBackground)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (chartData.isNotEmpty()) {
                HeroChart(
                    points = chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                )
            }

            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.label_net_worth_upper),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = netWorth,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B2633))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = PositiveGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = percentage,
                            color = PositiveGreen,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    if (change.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.label_this_month_suffix, change),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "MAY '25", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(text = "NOV", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(text = "MAY '26", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun HeroChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val progress = animationProgress.value
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val width = size.width
        val height = size.height

        val visibleCount = (points.size * progress).toInt().coerceAtLeast(2)
        val visible = points.take(visibleCount)

        val path = Path()
        val fillPath = Path()
        visible.forEachIndexed { i, pt ->
            val x = pt.x * width
            val y = height - (pt.y * height)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (i == visible.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(colors = listOf(PositiveGreen.copy(alpha = 0.3f), Color.Transparent))
        )
        drawPath(path = path, color = PositiveGreen, style = Stroke(width = 3.dp.toPx()))
        val lastPt = visible.last()
        drawCircle(
            color = PositiveGreen,
            radius = 4.dp.toPx(),
            center = Offset(lastPt.x * width, height - (lastPt.y * height))
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetWorthHeroCardPreview() {
    NetWorthTheme {
        NetWorthHeroCard(
            netWorth = "€18,200",
            change = "€850",
            percentage = "+ 4.9 %",
            isPositive = true,
            chartData = listOf(
                ChartPoint(0f, 0.2f), ChartPoint(0.2f, 0.3f),
                ChartPoint(0.4f, 0.25f), ChartPoint(0.6f, 0.5f),
                ChartPoint(0.8f, 0.6f), ChartPoint(1f, 0.8f)
            )
        )
    }
}
