package com.agcoding.networkapp.savings.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

/** Bar filled with the share of the salary that is left to save after fixed expenses. */
@Composable
internal fun SavingsProgressBar(
    savingsShare: Float,
    isDeficit: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColorScheme.current
    LinearProgressIndicator(
        progress = { savingsShare },
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = PositiveGreen,
        trackColor = if (isDeficit) colors.statusError.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
internal fun SavingsRateLabel(
    ratePercent: Int?,
    isDeficit: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColorScheme.current
    Text(
        text = when {
            isDeficit          -> stringResource(R.string.savings_planner_deficit)
            ratePercent != null -> stringResource(R.string.savings_planner_rate, ratePercent)
            else               -> ""
        },
        style = MaterialTheme.typography.labelSmall,
        color = if (isDeficit) colors.statusError else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
