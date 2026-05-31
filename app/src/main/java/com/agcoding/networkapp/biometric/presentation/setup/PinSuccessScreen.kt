package com.agcoding.networkapp.biometric.presentation.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinSuccessScreen(onComplete: () -> Unit) {
    val colors = LocalAppColorScheme.current

    val circleScale   = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val contentAlpha  = remember { Animatable(0f) }
    var showButton    by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            // nwPop: 0 → overshoot 1.12 at 60% → settle 1.0
            circleScale.animateTo(
                targetValue   = 1f,
                animationSpec = keyframes {
                    durationMillis = 450
                    0f    at 0   with LinearEasing
                    1.12f at 270 with LinearEasing
                    1f    at 450
                },
            )
        }
        delay(180)
        checkProgress.animateTo(
            targetValue   = 1f,
            animationSpec = tween(durationMillis = 520, easing = LinearOutSlowInEasing),
        )
        contentAlpha.animateTo(
            targetValue   = 1f,
            animationSpec = tween(durationMillis = 350),
        )
        delay(300)
        showButton = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(100.dp))

        // Animated circle + checkmark
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                scale(circleScale.value) {
                    drawCircle(color = colors.statusSuccess)
                }
            }
            Canvas(modifier = Modifier.size(140.dp)) {
                val strokeWidth = 10.dp.toPx()
                val checkPath = Path().apply {
                    val w = size.width
                    val h = size.height
                    moveTo(w * 0.27f, h * 0.51f)
                    lineTo(w * 0.44f, h * 0.68f)
                    lineTo(w * 0.73f, h * 0.34f)
                }
                val measurer = PathMeasure()
                measurer.setPath(checkPath, false)
                val animatedPath = Path()
                measurer.getSegment(0f, measurer.length * checkProgress.value, animatedPath, true)
                drawPath(
                    path  = animatedPath,
                    color = Color.White,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        Text(
            text       = stringResource(R.string.pin_success_title),
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = colors.contentPrimary,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.alpha(contentAlpha.value),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = stringResource(R.string.pin_success_subtitle),
            style     = MaterialTheme.typography.bodyMedium,
            color     = colors.contentSecondary,
            textAlign = TextAlign.Center,
            modifier  = Modifier.alpha(contentAlpha.value),
        )

        Spacer(Modifier.weight(1f))

        AnimatedVisibility(
            visible = showButton,
            enter   = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 },
        ) {
            Button(
                onClick  = onComplete,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = colors.actionPrimary),
            ) {
                Text(
                    text       = stringResource(R.string.btn_lets_start),
                    color      = colors.actionContent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
