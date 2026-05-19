package com.agcoding.networkapp.analytics.presentation.components

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.analytics.presentation.AccountComparisonLine
import com.agcoding.networkapp.shared.ui.theme.DarkBackground

@Composable
fun AccountComparisonCard(
    lines: List<AccountComparisonLine>,
    modifier: Modifier = Modifier,
) {
    if (lines.size < 2) return

    val animProgress = remember(lines) { Animatable(0f) }
    LaunchedEffect(lines) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBackground),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.analytics_comparison_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.analytics_comparison_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                letterSpacing = 0.5.sp,
            )

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                val progress = animProgress.value
                Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 4.dp)) {
                    val w    = size.width
                    val h    = size.height
                    val vPad = h * 0.06f

                    lines.forEach { line ->
                        val color = try { Color(android.graphics.Color.parseColor(line.colorHex)) }
                                    catch (e: Exception) { Color.Gray }
                        val visible = (line.points.size * progress).toInt().coerceAtLeast(2)
                        val pts = line.points.take(visible)

                        val path = Path()
                        pts.forEachIndexed { i, pt ->
                            val x = pt.x * w
                            val y = vPad + (1f - pt.y) * (h - 2 * vPad)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path  = path,
                            color = color,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                        // End dot
                        val last = pts.last()
                        drawCircle(
                            color  = color,
                            radius = 4.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(
                                last.x * w,
                                vPad + (1f - last.y) * (h - 2 * vPad),
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                lines.forEach { line ->
                    val color = try { Color(android.graphics.Color.parseColor(line.colorHex)) }
                                catch (e: Exception) { Color.Gray }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(color),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = line.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                        )
                    }
                }
            }
        }
    }
}
