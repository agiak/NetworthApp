package com.agcoding.networkapp.cloud.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.cloud.domain.model.CloudConnectionStatus
import com.agcoding.networkapp.cloud.domain.model.CloudMode
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen
import java.text.DateFormat
import java.util.Date

private val PendingAmber = Color(0xFFF59E0B)

/** "● Online · synced 14:32" / "● Offline mode · only on this phone", opens Cloud & Sync. */
@Composable
fun CloudStatusCard(
    status: CloudConnectionStatus?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mode = status?.mode ?: CloudMode.OFFLINE_MODE
    val dotColor = when (mode) {
        CloudMode.ONLINE, CloudMode.SYNCING           -> if (status?.hasPendingChanges == true) PendingAmber else PositiveGreen
        CloudMode.NO_INTERNET, CloudMode.SETUP_INCOMPLETE -> PendingAmber
        CloudMode.SYNC_ERROR                          -> LocalAppColorScheme.current.statusError
        CloudMode.OFFLINE_MODE                        -> Color.Gray
    }
    val title = stringResource(
        when (mode) {
            CloudMode.ONLINE, CloudMode.SYNCING, CloudMode.SYNC_ERROR -> R.string.cloud_mode_online
            CloudMode.NO_INTERNET                                     -> R.string.cloud_mode_offline
            CloudMode.OFFLINE_MODE, CloudMode.SETUP_INCOMPLETE        -> R.string.cloud_mode_offline_mode
        }
    )
    val detail = when (mode) {
        CloudMode.OFFLINE_MODE     -> stringResource(R.string.cloud_detail_offline_mode)
        CloudMode.SETUP_INCOMPLETE -> stringResource(R.string.cloud_detail_setup_incomplete)
        CloudMode.SYNCING          -> stringResource(R.string.cloud_detail_syncing)
        CloudMode.SYNC_ERROR       -> stringResource(R.string.cloud_detail_error)
        CloudMode.NO_INTERNET      -> stringResource(
            if (status?.hasPendingChanges == true) R.string.cloud_detail_no_internet_pending else R.string.cloud_detail_no_internet
        )
        CloudMode.ONLINE -> {
            val lastSyncAt = status?.lastSyncAt
            when {
                status?.hasPendingChanges == true -> stringResource(R.string.cloud_detail_pending)
                lastSyncAt != null                -> stringResource(
                    R.string.cloud_detail_synced,
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastSyncAt)),
                )
                else                              -> stringResource(R.string.cloud_status_never)
            }
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = dotColor.copy(alpha = 0.15f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = if (mode == CloudMode.OFFLINE_MODE) "📱" else "☁️", fontSize = 20.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mode == CloudMode.SYNC_ERROR) LocalAppColorScheme.current.statusError else Color.Gray,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cloud_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
