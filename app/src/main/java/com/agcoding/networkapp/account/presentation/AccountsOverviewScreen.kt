package com.agcoding.networkapp.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme

private const val MAIN_ACCOUNT_ID = 1L

@Composable
fun AccountsOverviewScreen(
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToAccountDetail: (Long) -> Unit = {},
    viewModel: AccountsOverviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColorScheme.current
    var accountToDelete by remember { mutableStateOf<AccountUiModel?>(null) }

    // Delete confirmation dialog
    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text(stringResource(R.string.delete_account_confirm_title), fontWeight = FontWeight.Bold) },
            text  = { Text(stringResource(R.string.delete_account_confirm_message, account.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onIntent(AccountsOverviewIntent.DeleteAccount(account.id))
                    accountToDelete = null
                }) {
                    Text(stringResource(R.string.btn_delete_entry), color = colors.statusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(20.dp),
        )
    }

    // Edit account dialog
    uiState.editingAccount?.let { account ->
        EditAccountDialog(
            account = account,
            onDismiss = { viewModel.onIntent(AccountsOverviewIntent.DismissEdit) },
            onSave = { name, colorHex ->
                viewModel.onIntent(AccountsOverviewIntent.SaveEdit(account.id, name, colorHex))
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick           = onNavigateToCreateAccount,
                containerColor    = colors.actionPrimary,
                contentColor      = colors.actionContent,
                shape             = CircleShape,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_create_account))
            }
        },
        topBar = {
            Text(
                text = stringResource(R.string.title_accounts),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    ) { padding ->
        if (uiState.accounts.isEmpty() && !uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.label_no_accounts),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.accounts.size > 1) {
                item { TotalNetWorthCard(total = uiState.totalNetWorth) }
            }
            // Best / worst this year
            val best  = uiState.bestAccount
            val worst = uiState.worstAccount
            if (best != null && worst != null && best.id != worst.id) {
                item {
                    BestWorstRow(
                        best  = best,
                        worst = worst,
                        onTap = onNavigateToAccountDetail,
                    )
                }
            }
            items(uiState.accounts, key = { it.id }) { account ->
                AccountCard(
                    account = account,
                    onDeleteClick = { accountToDelete = account },
                    onEditClick   = { viewModel.onIntent(AccountsOverviewIntent.StartEdit(account)) },
                    onTap         = { onNavigateToAccountDetail(account.id) },
                    canDelete     = account.id != MAIN_ACCOUNT_ID,
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun EditAccountDialog(
    account: AccountUiModel,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(account.name) }
    var selectedColor by rememberSaveable { mutableStateOf(account.colorHex) }
    val colors = LocalAppColorScheme.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.title_edit_account), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_account_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.label_account_color),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Account.PRESET_COLORS.forEach { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) }
                                        catch (e: Exception) { colors.actionPrimary }
                            val isSelected = hex == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, selectedColor) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.btn_save), color = colors.actionPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun TotalNetWorthCard(total: String) {
    val colors = LocalAppColorScheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.backgroundCard),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.label_total_net_worth),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = total,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun BestWorstRow(
    best: AccountPerformance,
    worst: AccountPerformance,
    onTap: (Long) -> Unit,
) {
    val colors = LocalAppColorScheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(best to true, worst to false).forEach { (perf, isBest) ->
            val accentColor = try { Color(android.graphics.Color.parseColor(perf.colorHex)) }
                              catch (e: Exception) { colors.actionPrimary }
            val valueColor = if (isBest) colors.statusSuccess else colors.statusError
            Card(
                onClick = { onTap(perf.id) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isBest) stringResource(R.string.label_best_account)
                               else stringResource(R.string.label_worst_account),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = perf.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = perf.growthPctFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = valueColor,
                    )
                    Text(
                        text = perf.growthAbsFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: AccountUiModel,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onTap: () -> Unit,
    canDelete: Boolean,
) {
    val colors = LocalAppColorScheme.current
    val accentColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        colors.actionPrimary
    }
    val changeColor = if (account.isPositiveChange) colors.statusSuccess else colors.statusError

    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = account.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = stringResource(R.string.label_entries_count, account.entryCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = account.currentBalance, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (account.entryCount > 0) {
                    Text(text = account.change, style = MaterialTheme.typography.bodySmall, color = changeColor, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.width(4.dp))

            IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }

            if (canDelete) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = colors.statusError, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
