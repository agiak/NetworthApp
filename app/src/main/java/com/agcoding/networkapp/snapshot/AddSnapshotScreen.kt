package com.agcoding.networkapp.snapshot

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.home.presentation.components.EntryDatePickerDialog
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AddSnapshotScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddSnapshotViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onNavigateBack()
    }

    AddSnapshotContent(
        uiState     = uiState,
        onIntent    = viewModel::onIntent,
        onClose     = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSnapshotContent(
    uiState: AddSnapshotUiState,
    onIntent: (AddSnapshotIntent) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalAppColorScheme.current
    var showDatePicker by remember { mutableStateOf(false) }

    val today     = LocalDate.now()
    val yesterday = today.minusDays(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(56.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text  = stringResource(R.string.title_new_snapshot),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.contentSecondary,
                    letterSpacing = 1.sp,
                )
                Text(
                    text       = stringResource(R.string.subtitle_net_worth_question),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = colors.contentPrimary,
                )
            }
            IconButton(
                onClick  = onClose,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.backgroundSecondary),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colors.contentPrimary,
                )
            }
        }

        if (uiState.accounts.size > 1) {
            Spacer(Modifier.height(16.dp))
            AccountSelector(
                accounts = uiState.accounts,
                selectedAccountId = uiState.selectedAccountId,
                onAccountSelected = { onIntent(AddSnapshotIntent.SelectAccount(it)) },
            )
        }

        Spacer(Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text       = uiState.currencySymbol,
                style      = MaterialTheme.typography.displaySmall,
                color      = colors.contentSecondary.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text       = if (uiState.entryInput.isEmpty()) "0" else uiState.entryInput,
                style      = MaterialTheme.typography.displayLarge,
                color      = if (uiState.entryInput.isEmpty()) colors.contentSecondary.copy(alpha = 0.4f) else colors.contentPrimary,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DateChip(
                label      = stringResource(R.string.chip_today),
                isSelected = uiState.selectedDate == today,
                onClick    = { onIntent(AddSnapshotIntent.UpdateDate(today)) },
            )
            DateChip(
                label      = stringResource(R.string.chip_yesterday),
                isSelected = uiState.selectedDate == yesterday,
                onClick    = { onIntent(AddSnapshotIntent.UpdateDate(yesterday)) },
            )
            DateChip(
                label      = stringResource(R.string.chip_pick_date),
                isSelected = uiState.selectedDate != today && uiState.selectedDate != yesterday,
                onClick    = { showDatePicker = true },
                icon       = Icons.Default.DateRange,
            )
        }

        Spacer(Modifier.height(8.dp))

        val displayDate = when (uiState.selectedDate) {
            today     -> stringResource(R.string.chip_today)
            yesterday -> stringResource(R.string.chip_yesterday)
            else      -> uiState.selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
        }
        Text(
            text     = displayDate,
            style    = MaterialTheme.typography.bodySmall,
            color    = colors.contentSecondary,
            modifier = Modifier.padding(start = 4.dp),
        )

        Spacer(Modifier.height(16.dp))

        androidx.compose.material3.OutlinedTextField(
            value = uiState.noteInput,
            onValueChange = { onIntent(AddSnapshotIntent.UpdateNote(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(stringResource(R.string.hint_note), style = MaterialTheme.typography.bodyMedium, color = colors.contentSecondary)
            },
            maxLines = 2,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.actionPrimary,
                unfocusedBorderColor = colors.contentSecondary.copy(alpha = 0.3f),
                focusedTextColor = colors.contentPrimary,
                unfocusedTextColor = colors.contentPrimary,
            ),
        )

        Spacer(Modifier.height(16.dp))

        val keyRows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "DEL"),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            keyRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { key ->
                        KeypadButton(
                            label    = key,
                            modifier = Modifier.weight(1f),
                            onClick  = {
                                when (key) {
                                    "DEL" -> if (uiState.entryInput.isNotEmpty()) onIntent(AddSnapshotIntent.UpdateInput(uiState.entryInput.dropLast(1)))
                                    "."   -> if (!uiState.entryInput.contains(".")) onIntent(AddSnapshotIntent.UpdateInput(uiState.entryInput + key))
                                    else  -> onIntent(AddSnapshotIntent.UpdateInput(uiState.entryInput + key))
                                }
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick  = { onIntent(AddSnapshotIntent.Save) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = colors.actionPrimary,
                contentColor           = colors.actionContent,
                disabledContainerColor = colors.actionPrimary.copy(alpha = 0.38f),
                disabledContentColor   = colors.actionContent.copy(alpha = 0.38f),
            ),
            enabled = uiState.entryInput.isNotEmpty() && !uiState.isSaving,
        ) {
            Text(
                text       = stringResource(R.string.btn_save_snapshot),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showDatePicker) {
        EntryDatePickerDialog(
            selectedDate   = uiState.selectedDate,
            onDateSelected = { onIntent(AddSnapshotIntent.UpdateDate(it)); showDatePicker = false },
            onDismiss      = { showDatePicker = false },
        )
    }
}

@Composable
private fun AccountSelector(
    accounts: List<Account>,
    selectedAccountId: Long,
    onAccountSelected: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        accounts.forEach { account ->
            val isSelected = account.id == selectedAccountId
            val accentColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) }
                              catch (e: Exception) { Color.Gray }
            Surface(
                onClick = { onAccountSelected(account.id) },
                shape   = RoundedCornerShape(20.dp),
                color   = if (isSelected) accentColor else Color.Transparent,
                border  = if (!isSelected) BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)) else null,
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text       = account.name,
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color      = if (isSelected) MaterialTheme.colorScheme.surface
                                     else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun DateChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val colors = LocalAppColorScheme.current
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(20.dp),
        color   = if (isSelected) colors.contentPrimary else androidx.compose.ui.graphics.Color.Transparent,
        border  = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, colors.contentSecondary.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) colors.backgroundPrimary else colors.contentPrimary,
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text       = label,
                style      = MaterialTheme.typography.labelLarge,
                color      = if (isSelected) colors.backgroundPrimary else colors.contentPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun KeypadButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalAppColorScheme.current
    val view = LocalView.current
    Surface(
        onClick  = { view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); onClick() },
        shape    = RoundedCornerShape(12.dp),
        color    = colors.backgroundSecondary,
        modifier = modifier.height(60.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (label == "DEL") {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = colors.contentPrimary,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = colors.contentPrimary,
                )
            }
        }
    }
}
