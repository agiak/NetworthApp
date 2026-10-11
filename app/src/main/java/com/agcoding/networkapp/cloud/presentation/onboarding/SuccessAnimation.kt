package com.agcoding.networkapp.cloud.presentation.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val ConfettiColors = listOf(
    Color(0xFF76C893), Color(0xFF5B8DEF), Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFFA78BFA), Color(0xFF22B8CF),
)

/** A green circle that pops in, a tick that draws itself and a burst of confetti. */
@Composable
fun SuccessAnimation(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
) {
    val circle = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { circle.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        launch {
            kotlinx.coroutines.delay(250)
            tick.animateTo(1f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
        }
        launch {
            kotlinx.coroutines.delay(350)
            burst.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        }
    }

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension * 0.32f

        // Confetti flying outwards and fading
        val pieces = 14
        if (burst.value > 0f && burst.value < 1f) {
            for (i in 0 until pieces) {
                val angle = (i * 360f / pieces + i * 7f) * (Math.PI / 180f).toFloat()
                val distance = radius * (1.05f + 0.75f * burst.value) * (if (i % 2 == 0) 1f else 0.85f)
                val alpha = (1f - burst.value).coerceIn(0f, 1f)
                drawCircle(
                    color = ConfettiColors[i % ConfettiColors.size].copy(alpha = alpha),
                    radius = this.size.minDimension * (if (i % 3 == 0) 0.028f else 0.02f),
                    center = Offset(center.x + cos(angle) * distance, center.y + sin(angle) * distance),
                )
            }
        }

        // Soft halo + circle
        drawCircle(color = color.copy(alpha = 0.15f * circle.value), radius = radius * 1.3f * circle.value, center = center)
        drawCircle(color = color, radius = radius * circle.value, center = center)

        // Tick, drawn along its path
        if (tick.value > 0f) {
            val path = Path().apply {
                moveTo(center.x - radius * 0.42f, center.y + radius * 0.02f)
                lineTo(center.x - radius * 0.1f, center.y + radius * 0.34f)
                lineTo(center.x + radius * 0.46f, center.y - radius * 0.3f)
            }
            val measure = PathMeasure().apply { setPath(path, forceClosed = false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * tick.value, partial, startWithMoveTo = true)
            drawPath(
                path = partial,
                color = Color.White,
                style = Stroke(width = radius * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
