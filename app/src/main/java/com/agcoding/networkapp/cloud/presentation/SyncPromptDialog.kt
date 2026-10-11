package com.agcoding.networkapp.cloud.presentation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

/** Shown at launch while the data lives only on this phone. */
@Composable
fun SyncPromptDialog(
    onSync: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        icon = { Text("☁️", fontSize = 28.sp) },
        title = { Text(stringResource(R.string.cloud_prompt_title), fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = stringResource(R.string.cloud_prompt_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onSync) {
                Text(stringResource(R.string.cloud_prompt_sync), color = PositiveGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.cloud_prompt_later), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}
