package com.agcoding.networkapp.goal.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.goal.presentation.GoalStatus
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

@Composable
fun GoalStatusCard(
    status: GoalStatus,
    fasterByPercent: Int,
    yearsAtCurrentPace: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        GoalStatus.ON_TRACK, GoalStatus.GOAL_REACHED -> PositiveGreen
        GoalStatus.SLIGHTLY_BEHIND -> Color(0xFFF57C00)
        GoalStatus.FAR_FROM_GOAL -> MaterialTheme.colorScheme.error
    }
    val statusLabel = when (status) {
        GoalStatus.ON_TRACK -> stringResource(R.string.goal_status_on_track)
        GoalStatus.SLIGHTLY_BEHIND -> stringResource(R.string.goal_status_slightly_behind)
        GoalStatus.FAR_FROM_GOAL -> stringResource(R.string.goal_status_far)
        GoalStatus.GOAL_REACHED -> stringResource(R.string.goal_status_reached)
    }
    val insight = when (status) {
        GoalStatus.ON_TRACK -> stringResource(R.string.goal_insight_on_track)
        GoalStatus.SLIGHTLY_BEHIND -> stringResource(R.string.goal_insight_slightly_behind, fasterByPercent)
        GoalStatus.FAR_FROM_GOAL -> stringResource(R.string.goal_insight_far, fasterByPercent)
        GoalStatus.GOAL_REACHED -> stringResource(R.string.goal_insight_reached)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = statusColor, modifier = Modifier.size(10.dp)) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = insight,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (status != GoalStatus.GOAL_REACHED && yearsAtCurrentPace.isNotEmpty() && yearsAtCurrentPace != "—") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.goal_at_pace_label) + ": " + yearsAtCurrentPace,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalStatusCardPreview() {
    NetWorthTheme {
        GoalStatusCard(
            status = GoalStatus.SLIGHTLY_BEHIND,
            fasterByPercent = 15,
            yearsAtCurrentPace = "4Y 6M"
        )
    }
}
