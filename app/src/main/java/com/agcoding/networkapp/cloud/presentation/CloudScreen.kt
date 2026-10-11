package com.agcoding.networkapp.cloud.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.cloud.domain.model.HouseholdMember
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen
import java.text.DateFormat
import java.util.Date

@Composable
fun CloudScreen(
    onNavigateBack: () -> Unit,
    viewModel: CloudViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CloudContent(uiState = uiState, onIntent = viewModel::onIntent, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudContent(
    uiState: CloudUiState,
    onIntent: (CloudIntent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cloud_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.message?.let { message ->
                item { MessageBanner(stringResource(message), isError = uiState.messageIsError) { onIntent(CloudIntent.ClearMessage) } }
            }
            when (uiState.step) {
                CloudStep.SIGNED_OUT -> {
                    item { Intro(stringResource(R.string.cloud_intro)) }
                    item { AuthCard(uiState, onIntent) }
                }
                CloudStep.NO_HOUSEHOLD -> {
                    item { SignedInAs(uiState.email, onSignOut = { onIntent(CloudIntent.SignOut) }) }
                    item { CreateHouseholdCard(uiState, onIntent) }
                    item { JoinHouseholdCard(uiState, onIntent) }
                }
                CloudStep.CONNECTED -> {
                    item { SyncCard(uiState, onIntent) }
                    item { MembersCard(uiState, onIntent) }
                    item { SignedInAs(uiState.email, onSignOut = { onIntent(CloudIntent.SignOut) }) }
                    item {
                        TextButton(onClick = { onIntent(CloudIntent.RequestLeave) }, enabled = !uiState.isBusy) {
                            Text(stringResource(R.string.cloud_leave), color = LocalAppColorScheme.current.statusError)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    uiState.inviteCode?.let { code -> InviteDialog(code, onDismiss = { onIntent(CloudIntent.DismissInvite) }) }

    if (uiState.confirmLeave) {
        AlertDialog(
            onDismissRequest = { onIntent(CloudIntent.DismissLeave) },
            title = { Text(stringResource(R.string.cloud_leave_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.cloud_leave_body)) },
            confirmButton = {
                TextButton(onClick = { onIntent(CloudIntent.ConfirmLeave) }) {
                    Text(stringResource(R.string.cloud_leave), color = LocalAppColorScheme.current.statusError)
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(CloudIntent.DismissLeave) }) { Text(stringResource(R.string.btn_cancel)) }
            },
        )
    }
}

@Composable
private fun Intro(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun CloudCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

@Composable
private fun CardTitle(emoji: String, title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AuthCard(uiState: CloudUiState, onIntent: (CloudIntent) -> Unit) {
    CloudCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !uiState.isSignUp,
                onClick = { onIntent(CloudIntent.SetAuthMode(signUp = false)) },
                label = { Text(stringResource(R.string.cloud_sign_in)) },
                colors = chipColors(),
            )
            FilterChip(
                selected = uiState.isSignUp,
                onClick = { onIntent(CloudIntent.SetAuthMode(signUp = true)) },
                label = { Text(stringResource(R.string.cloud_sign_up)) },
                colors = chipColors(),
            )
        }
        if (uiState.isSignUp) {
            CloudField(
                value = uiState.nameInput,
                onValueChange = { onIntent(CloudIntent.UpdateName(it)) },
                label = stringResource(R.string.cloud_name),
                capitalization = KeyboardCapitalization.Words,
            )
        }
        CloudField(
            value = uiState.emailInput,
            onValueChange = { onIntent(CloudIntent.UpdateEmail(it)) },
            label = stringResource(R.string.cloud_email),
            keyboardType = KeyboardType.Email,
        )
        CloudField(
            value = uiState.passwordInput,
            onValueChange = { onIntent(CloudIntent.UpdatePassword(it)) },
            label = stringResource(R.string.cloud_password),
            keyboardType = KeyboardType.Password,
            isPassword = true,
            supportingText = if (uiState.isSignUp) stringResource(R.string.cloud_password_hint, CloudUiState.MIN_PASSWORD) else null,
        )
        PrimaryButton(
            text = stringResource(if (uiState.isSignUp) R.string.cloud_sign_up else R.string.cloud_sign_in),
            enabled = uiState.canSubmitAuth,
            busy = uiState.isBusy,
            onClick = { onIntent(CloudIntent.SubmitAuth) },
        )
    }
}

@Composable
private fun SignedInAs(email: String, onSignOut: () -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.cloud_signed_in_as, email),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSignOut) { Text(stringResource(R.string.cloud_sign_out)) }
    }
}

@Composable
private fun CreateHouseholdCard(uiState: CloudUiState, onIntent: (CloudIntent) -> Unit) {
    CloudCard {
        CardTitle("🏡", stringResource(R.string.cloud_create_title), stringResource(R.string.cloud_create_subtitle))
        CloudField(
            value = uiState.householdNameInput,
            onValueChange = { onIntent(CloudIntent.UpdateHouseholdName(it)) },
            label = stringResource(R.string.cloud_household_name),
            capitalization = KeyboardCapitalization.Words,
        )
        PrimaryButton(
            text = stringResource(R.string.cloud_create_button),
            enabled = !uiState.isBusy && uiState.householdNameInput.isNotBlank(),
            busy = uiState.isBusy,
            onClick = { onIntent(CloudIntent.CreateHousehold) },
        )
    }
}

@Composable
private fun JoinHouseholdCard(uiState: CloudUiState, onIntent: (CloudIntent) -> Unit) {
    CloudCard {
        CardTitle("🔑", stringResource(R.string.cloud_join_title), stringResource(R.string.cloud_join_subtitle))
        CloudField(
            value = uiState.inviteCodeInput,
            onValueChange = { onIntent(CloudIntent.UpdateInviteCode(it)) },
            label = stringResource(R.string.cloud_invite_code),
            capitalization = KeyboardCapitalization.Characters,
            monospace = true,
        )
        Text(
            text = stringResource(R.string.cloud_join_warning),
            style = MaterialTheme.typography.labelSmall,
            color = LocalAppColorScheme.current.statusError,
        )
        OutlinedButton(
            onClick = { onIntent(CloudIntent.JoinHousehold) },
            enabled = !uiState.isBusy && uiState.inviteCodeInput.length == INVITE_LENGTH,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(R.string.cloud_join_button), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SyncCard(uiState: CloudUiState, onIntent: (CloudIntent) -> Unit) {
    CloudCard {
        CardTitle("☁️", uiState.householdName, stringResource(R.string.cloud_connected_subtitle))
        val status = when {
            !uiState.isOnline    -> stringResource(
                if (uiState.hasPendingChanges) R.string.cloud_detail_no_internet_pending else R.string.cloud_detail_no_internet
            )
            uiState.isSyncing    -> stringResource(R.string.cloud_status_syncing)
            uiState.hasPendingChanges -> stringResource(R.string.cloud_detail_pending)
            uiState.syncFailed   -> stringResource(R.string.cloud_status_failed)
            uiState.lastSyncAt != null -> stringResource(
                R.string.cloud_status_synced,
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(uiState.lastSyncAt)),
            )
            else                 -> stringResource(R.string.cloud_status_never)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (uiState.isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.syncFailed) LocalAppColorScheme.current.statusError else MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.cloud_auto_sync_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(
            text = stringResource(R.string.cloud_sync_now),
            enabled = !uiState.isBusy && !uiState.isSyncing && uiState.isOnline,
            busy = uiState.isBusy,
            onClick = { onIntent(CloudIntent.SyncNow) },
        )
    }
}

@Composable
private fun MembersCard(uiState: CloudUiState, onIntent: (CloudIntent) -> Unit) {
    CloudCard {
        CardTitle("👥", stringResource(R.string.cloud_members_title))
        uiState.members.forEachIndexed { index, member ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            MemberRow(member)
        }
        OutlinedButton(
            onClick = { onIntent(CloudIntent.CreateInvite) },
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(stringResource(R.string.cloud_invite_button), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MemberRow(member: HouseholdMember) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = member.displayName.ifBlank { stringResource(R.string.cloud_member_unnamed) } +
                if (member.isMe) " " + stringResource(R.string.cloud_member_you) else "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (member.isOwner) {
            Text(
                text = stringResource(R.string.cloud_member_owner),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InviteDialog(code: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val shareText = stringResource(R.string.cloud_invite_share_text, code)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cloud_invite_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.cloud_invite_body))
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                context.startActivity(Intent.createChooser(send, null))
            }) { Text(stringResource(R.string.cloud_invite_share), color = PositiveGreen, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("invite", code))
                onDismiss()
            }) { Text(stringResource(R.string.cloud_invite_copy)) }
        },
    )
}

@Composable
private fun MessageBanner(text: String, isError: Boolean, onDismiss: () -> Unit) {
    val color = if (isError) LocalAppColorScheme.current.statusError else PositiveGreen
    Surface(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
internal fun PrimaryButton(text: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onBackground,
            contentColor = MaterialTheme.colorScheme.background,
        ),
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.background)
        } else {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun CloudField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    isPassword: Boolean = false,
    monospace: Boolean = false,
    supportingText: String? = null,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        supportingText = if (supportingText != null) {
            { Text(supportingText) }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(if (passwordVisible) R.string.cloud_password_hide else R.string.cloud_password_show),
                    )
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Next,
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PositiveGreen,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            cursorColor = PositiveGreen,
        ),
        textStyle = if (monospace) MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                    else MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
    selectedLabelColor = MaterialTheme.colorScheme.surface,
)

private const val INVITE_LENGTH = 8

@Preview(showBackground = true)
@Composable
private fun CloudConnectedPreview() {
    NetWorthTheme {
        CloudContent(
            uiState = CloudUiState(
                isLoading = false,
                step = CloudStep.CONNECTED,
                email = "anastasis@example.com",
                householdName = "Giakoumatos",
                isOwner = true,
                lastSyncAt = 1_791_000_000_000,
                members = listOf(
                    HouseholdMember("1", "Anastasis", isOwner = true, isMe = true),
                    HouseholdMember("2", "Xristina", isOwner = false, isMe = false),
                ),
            ),
            onIntent = {},
            onNavigateBack = {},
        )
    }
}
