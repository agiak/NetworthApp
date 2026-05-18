package com.agcoding.networkapp.biometric.presentation.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.tokens.AppIcons

@Composable
fun SecurityPromptScreen(
    onSetUpSecurity: () -> Unit,
    onSkip: () -> Unit,
) {
    val colors = LocalAppColorScheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))

        Icon(
            imageVector = AppIcons.Settings,
            contentDescription = null,
            tint = colors.actionPrimary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.security_setup_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.contentPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.security_setup_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.contentSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onSetUpSecurity,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.actionPrimary),
        ) {
            Text(
                text = stringResource(R.string.security_setup_btn_yes),
                color = colors.actionContent,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.security_setup_btn_skip),
                color = colors.contentSecondary,
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
