package com.agcoding.networkapp.biometric.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun PinDotsRow(
    pin: String,
    length: Int,
    accentColor: Color,
    errorColor: Color = Color.Transparent,
    hasError: Boolean = false,
) {
    val shakeOffset = remember { Animatable(0f) }
    val density = LocalDensity.current

    LaunchedEffect(hasError) {
        if (!hasError) return@LaunchedEffect
        // Shake sequence matching nwShake keyframes (dp → px)
        val toX = { dp: Float -> with(density) { dp.dp.toPx() } }
        for (target in listOf(-2f, 4f, -9f, 9f, -9f, 9f, -9f, 4f, -2f, 0f)) {
            shakeOffset.animateTo(toX(target), animationSpec = tween(50, easing = LinearEasing))
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
    ) {
        repeat(length) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            hasError           -> errorColor
                            index < pin.length -> accentColor
                            else               -> accentColor.copy(alpha = 0.25f)
                        }
                    )
            )
        }
    }
}

@Composable
fun PinKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    textColor: Color,
    keyColor: Color,
    extraLeftLabel: String? = null,
    onExtraLeft: (() -> Unit)? = null,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (extraLeftLabel != null) "EXTRA" else "", "0", "⌫"),
    )

    val view = LocalView.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { key ->
                    val isExtra = key == "EXTRA"
                    val displayKey = if (isExtra) extraLeftLabel ?: "" else key
                    val isEmpty = displayKey.isEmpty()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.6f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (!isEmpty) keyColor else Color.Transparent)
                            .clickable(enabled = !isEmpty) {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                when {
                                    key == "⌫"  -> onBackspace()
                                    isExtra     -> onExtraLeft?.invoke()
                                    else        -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!isEmpty) {
                            Text(
                                text = displayKey,
                                style = if (isExtra) MaterialTheme.typography.labelSmall
                                        else         MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                            )
                        }
                    }
                }
            }
        }
    }
}
