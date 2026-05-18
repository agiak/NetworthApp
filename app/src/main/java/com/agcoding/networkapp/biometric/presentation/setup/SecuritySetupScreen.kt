package com.agcoding.networkapp.biometric.presentation.setup

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.agcoding.networkapp.R
import com.agcoding.networkapp.biometric.presentation.PinDotsRow
import com.agcoding.networkapp.biometric.presentation.PinKeypad
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme

@Composable
fun SecuritySetupScreen(
    state: SecuritySetupState,
    onIntent: (SecuritySetupIntent) -> Unit,
) {
    val context = LocalContext.current
    val colors  = LocalAppColorScheme.current

    LaunchedEffect(state.triggerBiometricPrompt) {
        if (!state.triggerBiometricPrompt) return@LaunchedEffect
        val activity = context as? FragmentActivity ?: run {
            onIntent(SecuritySetupIntent.BiometricFailed); return@LaunchedEffect
        }
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onIntent(SecuritySetupIntent.BiometricSuccess)
            }
            override fun onAuthenticationFailed() { onIntent(SecuritySetupIntent.BiometricFailed) }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onIntent(SecuritySetupIntent.BiometricFailed)
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_prompt_title))
            .setSubtitle(context.getString(R.string.biometric_prompt_subtitle))
            .setNegativeButtonText(context.getString(R.string.biometric_setup_btn_no))
            .build()
        prompt.authenticate(info)
        onIntent(SecuritySetupIntent.BiometricPromptShown)
    }

    when (state.step) {

        // ── Ask Security ─────────────────────────────────────────────────────
        SecurityStep.AskSecurity -> SecurityPromptScreen(
            onSetUpSecurity = { onIntent(SecuritySetupIntent.SetUpSecurity) },
            onSkip          = { onIntent(SecuritySetupIntent.SkipSecurity) },
        )

        // ── PIN Entry (Enter + Confirm) ───────────────────────────────────────
        SecurityStep.EnterPin, SecurityStep.ConfirmPin -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.backgroundPrimary)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(80.dp))

                Text(
                    text = if (state.step == SecurityStep.EnterPin)
                        stringResource(R.string.setup_pin_title)
                    else
                        stringResource(R.string.setup_pin_confirm_title),
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = colors.contentPrimary,
                    textAlign  = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (state.step == SecurityStep.EnterPin)
                        stringResource(R.string.setup_pin_subtitle)
                    else
                        stringResource(R.string.setup_pin_confirm_subtitle),
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = colors.contentSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.weight(1f))

                PinDotsRow(
                    pin         = state.pin,
                    length      = SecuritySetupViewModel.PIN_LENGTH,
                    accentColor = colors.actionPrimary,
                    errorColor  = colors.statusError,
                    hasError    = state.pinMismatch,
                )
                if (state.pinMismatch) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = stringResource(R.string.setup_pin_mismatch),
                        color = colors.statusError,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(32.dp))
                PinKeypad(
                    onDigit     = { onIntent(SecuritySetupIntent.DigitEntered(it)) },
                    onBackspace = { onIntent(SecuritySetupIntent.Backspace) },
                    textColor   = colors.contentPrimary,
                    keyColor    = colors.backgroundSecondary,
                )
                Spacer(Modifier.height(32.dp))
            }
        }

        // ── Ask Biometric ─────────────────────────────────────────────────────
        SecurityStep.AskBiometric -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.backgroundPrimary)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(80.dp))

                Text(
                    text       = stringResource(R.string.biometric_setup_title),
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = colors.contentPrimary,
                    textAlign  = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text      = stringResource(R.string.biometric_setup_body),
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = colors.contentSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.weight(1f))

                Button(
                    onClick  = { onIntent(SecuritySetupIntent.BiometricYes) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = colors.actionPrimary),
                ) {
                    Text(
                        text       = stringResource(R.string.biometric_setup_btn_yes),
                        color      = colors.actionContent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick  = { onIntent(SecuritySetupIntent.BiometricNo) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text  = stringResource(R.string.biometric_setup_btn_no),
                        color = colors.contentPrimary,
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        // ── Success ───────────────────────────────────────────────────────────
        SecurityStep.Success -> PinSuccessScreen(
            onComplete = { onIntent(SecuritySetupIntent.LetsStart) },
        )

        SecurityStep.Done -> Box(
            Modifier.fillMaxSize().background(colors.backgroundPrimary)
        )
    }
}
