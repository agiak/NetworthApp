package com.agcoding.networkapp.cloud.presentation.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.cloud.presentation.CloudField
import com.agcoding.networkapp.cloud.presentation.PrimaryButton
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen

// ── Profile: the cloud account section ──────────────────────────────────────

/** Change password, sign out and delete account; a sign-in link while signed out. */
@Composable
fun CloudAccountSection(
    onOpenCloud: () -> Unit,
    viewModel: AccountSecurityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val onIntent = viewModel::onIntent

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.account_section_title).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column {
                if (state.isSignedIn) {
                    AccountRow(
                        icon = Icons.Default.Cloud,
                        title = stringResource(R.string.account_signed_in_as),
                        subtitle = state.email,
                        onClick = onOpenCloud,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    AccountRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.account_change_password),
                        onClick = { onIntent(AccountSecurityIntent.Open(AccountDialog.CHANGE_PASSWORD)) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    AccountRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = stringResource(R.string.cloud_sign_out),
                        onClick = { onIntent(AccountSecurityIntent.Open(AccountDialog.SIGN_OUT)) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    AccountRow(
                        icon = Icons.Default.DeleteForever,
                        title = stringResource(R.string.account_delete),
                        tint = LocalAppColorScheme.current.statusError,
                        onClick = { onIntent(AccountSecurityIntent.Open(AccountDialog.DELETE_ACCOUNT)) },
                    )
                } else {
                    AccountRow(
                        icon = Icons.Default.Cloud,
                        title = stringResource(R.string.account_sign_in_or_register),
                        subtitle = stringResource(R.string.account_signed_out_hint),
                        onClick = onOpenCloud,
                    )
                }
            }
        }
        // Outcome of the last action (the dialogs show their own errors)
        if (state.dialog == null && state.message != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(state.message!!),
                style = MaterialTheme.typography.bodySmall,
                color = if (state.messageIsError) LocalAppColorScheme.current.statusError else PositiveGreen,
            )
        }
    }

    AccountSecurityDialogs(state, onIntent)
}

@Composable
private fun AccountRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (tint == MaterialTheme.colorScheme.onSurface) PositiveGreen else tint)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = tint)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

// ── Forgot password link (sign-in forms) ────────────────────────────────────

/** "Forgot password?" under a sign-in form; opens the temporary password dialog. */
@Composable
fun ForgotPasswordLink(
    email: String,
    modifier: Modifier = Modifier,
    viewModel: AccountSecurityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TextButton(
        onClick = { viewModel.onIntent(AccountSecurityIntent.Open(AccountDialog.FORGOT_PASSWORD, email)) },
        modifier = modifier,
    ) {
        Text(stringResource(R.string.account_forgot_password), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (state.dialog == AccountDialog.FORGOT_PASSWORD) {
        ForgotPasswordDialog(state, viewModel::onIntent)
    }
}

// ── Dialogs ─────────────────────────────────────────────────────────────────

@Composable
private fun AccountSecurityDialogs(state: AccountSecurityUiState, onIntent: (AccountSecurityIntent) -> Unit) {
    when (state.dialog) {
        AccountDialog.CHANGE_PASSWORD -> ChangePasswordDialog(state, onIntent)
        AccountDialog.FORGOT_PASSWORD -> ForgotPasswordDialog(state, onIntent)
        AccountDialog.SIGN_OUT        -> SignOutDialog(state, onIntent)
        AccountDialog.DELETE_ACCOUNT  -> DeleteAccountDialog(state, onIntent)
        null                          -> Unit
    }
}

@Composable
private fun ChangePasswordDialog(state: AccountSecurityUiState, onIntent: (AccountSecurityIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(AccountSecurityIntent.Dismiss) },
        title = { Text(stringResource(R.string.account_change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CloudField(
                    value = state.currentPasswordInput,
                    onValueChange = { onIntent(AccountSecurityIntent.UpdateCurrentPassword(it)) },
                    label = stringResource(R.string.account_current_password),
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                )
                NewPasswordFields(state, onIntent)
                DialogMessage(state)
            }
        },
        confirmButton = {
            DialogButton(
                text = stringResource(R.string.btn_save),
                enabled = state.canChangePassword,
                busy = state.isBusy,
                onClick = { onIntent(AccountSecurityIntent.SubmitChangePassword) },
            )
        },
        dismissButton = { CancelButton(state, onIntent) },
    )
}

@Composable
private fun ForgotPasswordDialog(state: AccountSecurityUiState, onIntent: (AccountSecurityIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(AccountSecurityIntent.Dismiss) },
        title = { Text(stringResource(R.string.account_forgot_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.forgotSent) {
                    Text("📬", fontSize = 40.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text(
                        text = stringResource(R.string.account_forgot_sent, state.forgotEmailInput),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(stringResource(R.string.account_forgot_body), style = MaterialTheme.typography.bodyMedium)
                    CloudField(
                        value = state.forgotEmailInput,
                        onValueChange = { onIntent(AccountSecurityIntent.UpdateForgotEmail(it)) },
                        label = stringResource(R.string.cloud_email),
                        keyboardType = KeyboardType.Email,
                    )
                    DialogMessage(state)
                }
            }
        },
        confirmButton = {
            if (state.forgotSent) {
                TextButton(onClick = { onIntent(AccountSecurityIntent.Dismiss) }) { Text(stringResource(R.string.account_ok)) }
            } else {
                DialogButton(
                    text = stringResource(R.string.account_forgot_send),
                    enabled = state.canSendForgot,
                    busy = state.isBusy,
                    onClick = { onIntent(AccountSecurityIntent.SubmitForgotPassword) },
                )
            }
        },
        dismissButton = if (state.forgotSent) null else ({ CancelButton(state, onIntent) }),
    )
}

@Composable
private fun SignOutDialog(state: AccountSecurityUiState, onIntent: (AccountSecurityIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(AccountSecurityIntent.Dismiss) },
        title = { Text(stringResource(R.string.account_sign_out_title)) },
        text = { Text(stringResource(R.string.account_sign_out_body), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            DialogButton(
                text = stringResource(R.string.cloud_sign_out),
                enabled = !state.isBusy,
                busy = state.isBusy,
                onClick = { onIntent(AccountSecurityIntent.ConfirmSignOut) },
            )
        },
        dismissButton = { CancelButton(state, onIntent) },
    )
}

@Composable
private fun DeleteAccountDialog(state: AccountSecurityUiState, onIntent: (AccountSecurityIntent) -> Unit) {
    val danger = LocalAppColorScheme.current.statusError
    AlertDialog(
        onDismissRequest = { onIntent(AccountSecurityIntent.Dismiss) },
        title = { Text(stringResource(R.string.account_delete_title), color = danger) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.account_delete_body), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(R.string.account_delete_local_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CloudField(
                    value = state.currentPasswordInput,
                    onValueChange = { onIntent(AccountSecurityIntent.UpdateCurrentPassword(it)) },
                    label = stringResource(R.string.account_delete_password_label),
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                )
                DialogMessage(state)
            }
        },
        confirmButton = {
            DialogButton(
                text = stringResource(R.string.account_delete_confirm),
                enabled = state.canDeleteAccount,
                busy = state.isBusy,
                color = danger,
                onClick = { onIntent(AccountSecurityIntent.ConfirmDeleteAccount) },
            )
        },
        dismissButton = { CancelButton(state, onIntent) },
    )
}

// ── Forced change after signing in with a temporary password ────────────────

/** Covers the app until a new password is chosen (or the user signs out). */
@Composable
fun ForcedPasswordChangeScreen(state: AccountSecurityUiState, onIntent: (AccountSecurityIntent) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(48.dp))
                Text("🔑", fontSize = 56.sp)
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.account_forced_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.account_forced_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(28.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NewPasswordFields(state, onIntent)
                    DialogMessage(state)
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.account_forced_save),
                    enabled = state.canChangePassword,
                    busy = state.isBusy,
                    onClick = { onIntent(AccountSecurityIntent.SubmitChangePassword) },
                )
                TextButton(
                    onClick = { onIntent(AccountSecurityIntent.ConfirmSignOut) },
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cloud_sign_out), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Building blocks ─────────────────────────────────────────────────────────

@Composable
private fun NewPasswordFields(state: AccountSecurityUiState, onIntent: (AccountSecurityIntent) -> Unit) {
    CloudField(
        value = state.newPasswordInput,
        onValueChange = { onIntent(AccountSecurityIntent.UpdateNewPassword(it)) },
        label = stringResource(R.string.account_new_password),
        keyboardType = KeyboardType.Password,
        isPassword = true,
        supportingText = stringResource(R.string.cloud_password_hint, AccountSecurityUiState.MIN_PASSWORD),
    )
    CloudField(
        value = state.confirmPasswordInput,
        onValueChange = { onIntent(AccountSecurityIntent.UpdateConfirmPassword(it)) },
        label = stringResource(R.string.account_confirm_password),
        keyboardType = KeyboardType.Password,
        isPassword = true,
        supportingText = if (state.confirmPasswordInput.isNotEmpty() && !state.passwordsMatch) {
            stringResource(R.string.account_passwords_differ)
        } else null,
    )
}

@Composable
private fun DialogMessage(state: AccountSecurityUiState) {
    val message = state.message ?: return
    if (!state.messageIsError) return
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodySmall,
        color = LocalAppColorScheme.current.statusError,
    )
}

@Composable
private fun DialogButton(
    text: String,
    enabled: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    color: Color = PositiveGreen,
) {
    TextButton(onClick = onClick, enabled = enabled && !busy) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = color)
        } else {
            Text(text, fontWeight = FontWeight.Bold, color = if (enabled) color else Color.Gray)
        }
    }
}

@Composable
private fun CancelButton(state: AccountSecurityUiState, onIntent: (AccountSecurityIntent) -> Unit) {
    TextButton(onClick = { onIntent(AccountSecurityIntent.Dismiss) }, enabled = !state.isBusy) {
        Text(stringResource(R.string.account_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
