package com.agcoding.networkapp.cloud.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.cloud.domain.model.SyncStage
import com.agcoding.networkapp.cloud.domain.model.SyncSummary
import com.agcoding.networkapp.cloud.presentation.CloudField
import com.agcoding.networkapp.cloud.presentation.PrimaryButton
import com.agcoding.networkapp.cloud.presentation.account.ForgotPasswordLink
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen
import kotlin.math.roundToInt

/** Full-screen "sync your data" flow; covers the app until it's finished or skipped. */
@Composable
fun SyncOnboardingScreen(
    state: SyncOnboardingUiState,
    onIntent: (SyncOnboardingIntent) -> Unit,
    /** False inside the app's Scaffold, which already keeps clear of the system bars. */
    applySystemBarsPadding: Boolean = true,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "sync-onboarding-step",
            modifier = Modifier
                .fillMaxSize()
                .then(if (applySystemBarsPadding) Modifier.systemBarsPadding() else Modifier)
                .imePadding(),
        ) { step ->
            when (step) {
                SyncOnboardingStep.WELCOME   -> WelcomePage(onIntent)
                SyncOnboardingStep.INTRO     -> IntroPage(onIntent)
                SyncOnboardingStep.ACCOUNT   -> AccountPage(state, onIntent)
                SyncOnboardingStep.HOUSEHOLD -> HouseholdPage(state, onIntent)
                SyncOnboardingStep.SYNCING   -> SyncingPage(state)
                SyncOnboardingStep.SUCCESS   -> SuccessPage(state, onIntent)
                SyncOnboardingStep.ERROR     -> ErrorPage(state, onIntent)
            }
        }
    }
}

// ── Pages ───────────────────────────────────────────────────────────────────

/** First launch: sign in, register, or use the app on this phone only. */
@Composable
private fun WelcomePage(onIntent: (SyncOnboardingIntent) -> Unit) {
    PageColumn(
        bottom = {
            PrimaryButton(
                text = stringResource(R.string.cloud_sign_in),
                enabled = true,
                busy = false,
                onClick = { onIntent(SyncOnboardingIntent.ChooseSignIn) },
            )
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { onIntent(SyncOnboardingIntent.ChooseRegister) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.welcome_register),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TextButton(onClick = { onIntent(SyncOnboardingIntent.ContinueWithoutAccount) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.welcome_without_account), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    ) {
        Spacer(Modifier.height(40.dp))
        Box(modifier = Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = PositiveGreen.copy(alpha = 0.12f)) {}
            Text("💰", fontSize = 64.sp)
        }
        Spacer(Modifier.height(32.dp))
        Title(stringResource(R.string.welcome_title))
        Spacer(Modifier.height(12.dp))
        Body(stringResource(R.string.welcome_body))
        Spacer(Modifier.height(28.dp))
        Feature("🏡", stringResource(R.string.welcome_feature_household))
        Feature("📱", stringResource(R.string.sync_onboarding_feature_devices))
        Feature("🔒", stringResource(R.string.sync_onboarding_feature_private))
    }
}

@Composable
private fun IntroPage(onIntent: (SyncOnboardingIntent) -> Unit) {
    val pulse = rememberInfiniteTransition(label = "cloud-pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cloud-scale",
    )
    PageColumn(
        bottom = {
            PrimaryButton(
                text = stringResource(R.string.sync_onboarding_yes),
                enabled = true,
                busy = false,
                onClick = { onIntent(SyncOnboardingIntent.Start) },
            )
            TextButton(onClick = { onIntent(SyncOnboardingIntent.Skip) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sync_onboarding_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    ) {
        Spacer(Modifier.height(32.dp))
        Box(
            modifier = Modifier.size(132.dp).scale(scale),
            contentAlignment = Alignment.Center,
        ) {
            Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = PositiveGreen.copy(alpha = 0.12f)) {}
            Text("☁️", fontSize = 64.sp)
        }
        Spacer(Modifier.height(32.dp))
        Title(stringResource(R.string.sync_onboarding_title))
        Spacer(Modifier.height(12.dp))
        Body(stringResource(R.string.sync_onboarding_body))
        Spacer(Modifier.height(28.dp))
        Feature("🔒", stringResource(R.string.sync_onboarding_feature_private))
        Feature("📱", stringResource(R.string.sync_onboarding_feature_devices))
        Feature("⚡", stringResource(R.string.sync_onboarding_feature_auto))
    }
}

@Composable
private fun AccountPage(state: SyncOnboardingUiState, onIntent: (SyncOnboardingIntent) -> Unit) {
    PageColumn(
        onBack = { onIntent(SyncOnboardingIntent.Back) },
        bottom = {
            PrimaryButton(
                text = stringResource(if (state.isSignUp) R.string.cloud_sign_up else R.string.cloud_sign_in),
                enabled = state.canSubmitAuth,
                busy = state.isBusy,
                onClick = { onIntent(SyncOnboardingIntent.SubmitAuth) },
            )
        },
    ) {
        PageEmoji("👤")
        Title(
            stringResource(
                when {
                    !state.welcome   -> R.string.sync_onboarding_account_title
                    state.isSignUp   -> R.string.welcome_register
                    else             -> R.string.cloud_sign_in
                }
            )
        )
        Spacer(Modifier.height(8.dp))
        Body(
            stringResource(
                when {
                    !state.welcome   -> R.string.sync_onboarding_account_body
                    state.isSignUp   -> R.string.welcome_register_body
                    else             -> R.string.welcome_sign_in_body
                }
            )
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.isSignUp,
                onClick = { onIntent(SyncOnboardingIntent.SetSignUp(true)) },
                label = { Text(stringResource(R.string.cloud_sign_up)) },
                colors = chipColors(),
            )
            FilterChip(
                selected = !state.isSignUp,
                onClick = { onIntent(SyncOnboardingIntent.SetSignUp(false)) },
                label = { Text(stringResource(R.string.sync_onboarding_have_account)) },
                colors = chipColors(),
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.isSignUp) {
                CloudField(
                    value = state.nameInput,
                    onValueChange = { onIntent(SyncOnboardingIntent.UpdateName(it)) },
                    label = stringResource(R.string.cloud_name),
                    capitalization = KeyboardCapitalization.Words,
                )
            }
            CloudField(
                value = state.emailInput,
                onValueChange = { onIntent(SyncOnboardingIntent.UpdateEmail(it)) },
                label = stringResource(R.string.cloud_email),
                keyboardType = KeyboardType.Email,
            )
            CloudField(
                value = state.passwordInput,
                onValueChange = { onIntent(SyncOnboardingIntent.UpdatePassword(it)) },
                label = stringResource(R.string.cloud_password),
                keyboardType = KeyboardType.Password,
                isPassword = true,
                supportingText = if (state.isSignUp) stringResource(R.string.cloud_password_hint, SyncOnboardingUiState.MIN_PASSWORD) else null,
            )
        }
        if (!state.isSignUp) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                ForgotPasswordLink(email = state.emailInput)
            }
        }
        Message(state)
    }
}

@Composable
private fun HouseholdPage(state: SyncOnboardingUiState, onIntent: (SyncOnboardingIntent) -> Unit) {
    PageColumn(
        onBack = { onIntent(SyncOnboardingIntent.Back) },
        bottom = {
            PrimaryButton(
                text = stringResource(
                    when {
                        state.joinWithCode -> R.string.cloud_join_button
                        state.welcome      -> R.string.welcome_create_household
                        else               -> R.string.sync_onboarding_upload
                    }
                ),
                enabled = state.canSubmitHousehold,
                busy = state.isBusy,
                onClick = { onIntent(SyncOnboardingIntent.SubmitHousehold) },
            )
        },
    ) {
        PageEmoji("🏡")
        Title(stringResource(R.string.sync_onboarding_household_title))
        Spacer(Modifier.height(8.dp))
        Body(stringResource(R.string.sync_onboarding_household_body))
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !state.joinWithCode,
                onClick = { onIntent(SyncOnboardingIntent.SetJoinWithCode(false)) },
                label = { Text(stringResource(R.string.sync_onboarding_new_household)) },
                colors = chipColors(),
            )
            FilterChip(
                selected = state.joinWithCode,
                onClick = { onIntent(SyncOnboardingIntent.SetJoinWithCode(true)) },
                label = { Text(stringResource(R.string.sync_onboarding_have_code)) },
                colors = chipColors(),
            )
        }
        Spacer(Modifier.height(12.dp))
        if (state.joinWithCode) {
            CloudField(
                value = state.inviteCodeInput,
                onValueChange = { onIntent(SyncOnboardingIntent.UpdateInviteCode(it)) },
                label = stringResource(R.string.cloud_invite_code),
                capitalization = KeyboardCapitalization.Characters,
                monospace = true,
            )
            // Nothing to lose on first launch
            if (!state.welcome) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.cloud_join_warning),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalAppColorScheme.current.statusError,
                )
            }
        } else {
            CloudField(
                value = state.householdNameInput,
                onValueChange = { onIntent(SyncOnboardingIntent.UpdateHouseholdName(it)) },
                label = stringResource(R.string.cloud_household_name),
                capitalization = KeyboardCapitalization.Words,
            )
        }
        Message(state)
    }
}

@Composable
private fun SyncingPage(state: SyncOnboardingUiState) {
    val progress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "sync-progress",
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "${(progress * 100).roundToInt()}%",
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = PositiveGreen,
        )
        Spacer(Modifier.height(20.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = PositiveGreen,
            trackColor = PositiveGreen.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(32.dp))
        AnimatedContent(
            targetState = state.stage,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
            },
            label = "sync-stage",
        ) { stage ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stage.emoji, fontSize = 36.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(if (state.downloading) stage.downloadMessageRes else stage.messageRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.sync_onboarding_keep_open),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SuccessPage(state: SyncOnboardingUiState, onIntent: (SyncOnboardingIntent) -> Unit) {
    val summary = state.summary
    PageColumn(
        bottom = {
            PrimaryButton(
                text = stringResource(R.string.sync_onboarding_continue),
                enabled = true,
                busy = false,
                onClick = { onIntent(SyncOnboardingIntent.Continue) },
            )
        },
    ) {
        Spacer(Modifier.height(24.dp))
        SuccessAnimation(color = PositiveGreen, size = 180.dp)
        Spacer(Modifier.height(16.dp))
        Title(stringResource(if (state.downloading) R.string.welcome_success_title else R.string.sync_onboarding_success_title))
        Spacer(Modifier.height(12.dp))
        Body(stringResource(if (state.downloading) R.string.welcome_success_body else R.string.sync_onboarding_success_body))
        if (state.downloading && summary != null && summary.received > 0) {
            Spacer(Modifier.height(24.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryLine("👤", stringResource(R.string.sync_onboarding_summary_accounts), summary.accountsReceived)
                    SummaryLine("📈", stringResource(R.string.sync_onboarding_summary_entries), summary.entriesReceived)
                    SummaryLine("💸", stringResource(R.string.sync_onboarding_summary_expenses), summary.expensesReceived)
                    SummaryLine("🧠", stringResource(R.string.sync_onboarding_summary_categories), summary.categoriesReceived)
                }
            }
        } else if (!state.downloading && summary != null && summary.uploaded + summary.received > 0) {
            Spacer(Modifier.height(24.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryLine("👤", stringResource(R.string.sync_onboarding_summary_accounts), summary.accountsUploaded)
                    SummaryLine("📈", stringResource(R.string.sync_onboarding_summary_entries), summary.entriesUploaded)
                    SummaryLine("💸", stringResource(R.string.sync_onboarding_summary_expenses), summary.expensesUploaded)
                    SummaryLine("🧠", stringResource(R.string.sync_onboarding_summary_categories), summary.categoriesUploaded)
                    if (summary.received > 0) {
                        SummaryLine("⬇️", stringResource(R.string.sync_onboarding_summary_received), summary.received)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorPage(state: SyncOnboardingUiState, onIntent: (SyncOnboardingIntent) -> Unit) {
    PageColumn(
        bottom = {
            PrimaryButton(
                text = stringResource(R.string.sync_onboarding_retry),
                enabled = true,
                busy = false,
                onClick = { onIntent(SyncOnboardingIntent.Retry) },
            )
            TextButton(onClick = { onIntent(SyncOnboardingIntent.Skip) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sync_onboarding_skip_for_now), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    ) {
        PageEmoji("😕")
        Title(stringResource(R.string.sync_onboarding_error_title))
        Spacer(Modifier.height(12.dp))
        Body(state.message?.let { stringResource(it) } ?: stringResource(R.string.cloud_error_unknown))
        Spacer(Modifier.height(8.dp))
        Body(stringResource(if (state.downloading) R.string.welcome_error_body else R.string.sync_onboarding_error_body))
    }
}

// ── Building blocks ─────────────────────────────────────────────────────────

/** Scrollable page content with buttons pinned at the bottom. */
@Composable
private fun PageColumn(
    onBack: (() -> Unit)? = null,
    bottom: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) { bottom() }
    }
}

@Composable
private fun PageEmoji(emoji: String) {
    Spacer(Modifier.height(16.dp))
    Text(emoji, fontSize = 52.sp)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Feature(emoji: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface) {
            Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 18.sp) }
        }
        Spacer(Modifier.width(14.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SummaryLine(emoji: String, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(count.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = PositiveGreen)
    }
}

@Composable
private fun Message(state: SyncOnboardingUiState) {
    val message = state.message ?: return
    Spacer(Modifier.height(16.dp))
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = (if (state.messageIsError) LocalAppColorScheme.current.statusError else PositiveGreen).copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
    selectedLabelColor = MaterialTheme.colorScheme.surface,
)

private val SyncStage.emoji: String
    get() = when (this) {
        SyncStage.CONNECTING -> "☁️"
        SyncStage.CHECKING   -> "🔍"
        SyncStage.ACCOUNTS   -> "👤"
        SyncStage.ENTRIES    -> "📈"
        SyncStage.EXPENSES   -> "💸"
        SyncStage.CATEGORIES -> "🧠"
        SyncStage.FINISHING  -> "✨"
        SyncStage.DONE       -> "✅"
    }

private val SyncStage.messageRes: Int
    get() = when (this) {
        SyncStage.CONNECTING -> R.string.sync_stage_connecting
        SyncStage.CHECKING   -> R.string.sync_stage_checking
        SyncStage.ACCOUNTS   -> R.string.sync_stage_accounts
        SyncStage.ENTRIES    -> R.string.sync_stage_entries
        SyncStage.EXPENSES   -> R.string.sync_stage_expenses
        SyncStage.CATEGORIES -> R.string.sync_stage_categories
        SyncStage.FINISHING  -> R.string.sync_stage_finishing
        SyncStage.DONE       -> R.string.sync_stage_done
    }

private val SyncStage.downloadMessageRes: Int
    get() = when (this) {
        SyncStage.CONNECTING -> R.string.sync_stage_connecting
        SyncStage.CHECKING   -> R.string.sync_stage_download_checking
        SyncStage.ACCOUNTS   -> R.string.sync_stage_download_accounts
        SyncStage.ENTRIES    -> R.string.sync_stage_download_entries
        SyncStage.EXPENSES   -> R.string.sync_stage_download_expenses
        SyncStage.CATEGORIES -> R.string.sync_stage_download_categories
        SyncStage.FINISHING  -> R.string.sync_stage_finishing
        SyncStage.DONE       -> R.string.sync_stage_done
    }

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    NetWorthTheme {
        SyncOnboardingScreen(state = SyncOnboardingUiState(welcome = true, step = SyncOnboardingStep.WELCOME), onIntent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncingPreview() {
    NetWorthTheme {
        SyncOnboardingScreen(
            state = SyncOnboardingUiState(visible = true, step = SyncOnboardingStep.SYNCING, stage = SyncStage.ENTRIES, progress = 0.56f),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SuccessPreview() {
    NetWorthTheme {
        SyncOnboardingScreen(
            state = SyncOnboardingUiState(
                visible = true,
                step = SyncOnboardingStep.SUCCESS,
                summary = SyncSummary(accountsUploaded = 2, entriesUploaded = 91, expensesUploaded = 11, categoriesUploaded = 3),
            ),
            onIntent = {},
        )
    }
}
