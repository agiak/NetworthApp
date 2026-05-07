package com.agcoding.networkapp.analytics.presentation.prediction.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.analytics.presentation.prediction.PredictionRange
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme

@Composable
fun PredictionRangeSelector(
    selectedRange: PredictionRange,
    onRangeSelect: (PredictionRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        PredictionRange.TWO_YEARS to stringResource(R.string.prediction_range_2y),
        PredictionRange.THREE_YEARS to stringResource(R.string.prediction_range_3y),
        PredictionRange.FIVE_YEARS to stringResource(R.string.prediction_range_5y),
        PredictionRange.TEN_YEARS to stringResource(R.string.prediction_range_10y),
        PredictionRange.FIFTEEN_YEARS to stringResource(R.string.prediction_range_15y)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (range, label) ->
            val isSelected = range == selectedRange
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                animationSpec = tween(200),
                label = "rangeBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(200),
                label = "rangeText"
            )

            Surface(
                onClick = { onRangeSelect(range) },
                shape = RoundedCornerShape(20.dp),
                color = bgColor,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 11.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PredictionRangeSelectorPreview() {
    NetWorthTheme {
        PredictionRangeSelector(
            selectedRange = PredictionRange.FIVE_YEARS,
            onRangeSelect = {}
        )
    }
}
