package com.agcoding.networkapp.biometric.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PinDotsRow(
    pin: String,
    length: Int,
    accentColor: Color,
    errorColor: Color = Color.Transparent,
    hasError: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(length) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            hasError         -> errorColor
                            index < pin.length -> accentColor
                            else             -> accentColor.copy(alpha = 0.25f)
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
