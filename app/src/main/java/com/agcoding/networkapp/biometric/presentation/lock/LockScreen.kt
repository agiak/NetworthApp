package com.agcoding.networkapp.biometric.presentation.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.agcoding.networkapp.R
import com.agcoding.networkapp.biometric.presentation.PinDotsRow
import com.agcoding.networkapp.biometric.presentation.PinKeypad
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme

@Composable
fun LockScreen(
    state: LockUiState,
    onIntent: (LockIntent) -> Unit,
) {
    val context = LocalContext.current
    val colors  = LocalAppColorScheme.current

    LaunchedEffect(Unit) {
        val deviceOk = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
        onIntent(LockIntent.Initialize(deviceOk))
    }

    LaunchedEffect(state.triggerBiometricPrompt) {
        if (!state.triggerBiometricPrompt) return@LaunchedEffect
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onIntent(LockIntent.BiometricSuccess)
            }
            override fun onAuthenticationFailed() { onIntent(LockIntent.BiometricFailed) }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onIntent(LockIntent.BiometricFailed)
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_prompt_title))
            .setSubtitle(context.getString(R.string.biometric_prompt_subtitle))
            .setNegativeButtonText(context.getString(R.string.biometric_prompt_negative_btn))
            .build()
        prompt.authenticate(info)
        onIntent(LockIntent.BiometricPromptShown)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(80.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = stringResource(R.string.app_name),
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = colors.contentPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = stringResource(R.string.lock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.contentSecondary,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PinDotsRow(
                pin         = state.pin,
                length      = LockViewModel.PIN_LENGTH,
                accentColor = colors.actionPrimary,
                errorColor  = colors.statusError,
                hasError    = state.pinError,
            )
            if (state.pinError) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = stringResource(R.string.lock_wrong_pin),
                    color = colors.statusError,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(32.dp))
            PinKeypad(
                onDigit      = { onIntent(LockIntent.DigitEntered(it)) },
                onBackspace  = { onIntent(LockIntent.Backspace) },
                textColor    = colors.contentPrimary,
                keyColor     = colors.backgroundSecondary,
                extraLeftLabel = if (state.showBiometricButton) stringResource(R.string.lock_use_biometric) else null,
                onExtraLeft  = { onIntent(LockIntent.BiometricRequested) },
            )
        }

        Spacer(Modifier.height(48.dp))
    }
}
