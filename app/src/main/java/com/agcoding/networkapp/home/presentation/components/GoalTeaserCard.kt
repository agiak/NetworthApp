package com.agcoding.networkapp.home.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun GoalTeaserCard(
    currentNetWorthRaw: Double,
    targetAmountRaw: Double,
    targetAmountFormatted: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (targetAmountRaw > 0)
        (currentNetWorthRaw / targetAmountRaw).toFloat().coerceIn(0f, 1f)
    else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(800),
        label = "goalTeaserProgress"
    )

    val progressPercent = "${(progressFraction * 100).toInt()}%"
    val motivationLabel = when {
        progressFraction >= 1.0f -> stringResource(R.string.home_goal_reached)
        progressFraction >= 0.75f -> stringResource(R.string.home_goal_almost_there)
        progressFraction >= 0.5f -> stringResource(R.string.home_goal_great_progress)
        progressFraction >= 0.25f -> stringResource(R.string.home_goal_keep_going)
        else -> stringResource(R.string.home_goal_getting_started)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎯", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.home_goal_progress_title).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = stringResource(R.string.home_goal_progress_label, progressPercent, targetAmountFormatted),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = motivationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = PositiveGreen
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PositiveGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalTeaserCardPreview() {
    NetWorthTheme {
        GoalTeaserCard(
            currentNetWorthRaw = 42000.0,
            targetAmountRaw = 100000.0,
            targetAmountFormatted = "€100,000",
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
