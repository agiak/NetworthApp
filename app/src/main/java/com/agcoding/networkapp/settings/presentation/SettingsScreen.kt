package com.agcoding.networkapp.settings.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.settings.domain.model.AppLanguage
import com.agcoding.networkapp.settings.domain.model.AppTheme
import com.agcoding.networkapp.settings.presentation.components.BackupRestoreSection
import com.agcoding.networkapp.settings.presentation.components.SettingsSectionHeader
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.theme.PositiveGreen
import java.time.LocalDate

@Composable
fun SettingsScreen(
    onNavigateToProfileEdit: () -> Unit,
    onNavigateToSetupPin: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToFixedExpenses: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        uiState = uiState,
        onIntent = { intent ->
            when (intent) {
                SettingsIntent.NavigateToFixedExpenses -> onNavigateToFixedExpenses()
                SettingsIntent.NavigateToOnboarding    -> onNavigateToOnboarding()
                SettingsIntent.NavigateToProfileEdit   -> onNavigateToProfileEdit()
                SettingsIntent.NavigateToSetupPin      -> onNavigateToSetupPin()
                else -> viewModel.onIntent(intent)
            }
        }
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Export JSON file picker
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { onIntent(SettingsIntent.ExportToUri(it)) } }

    // Export CSV file picker
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { onIntent(SettingsIntent.ExportCsvToUri(it)) } }

    // Import file picker
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onIntent(SettingsIntent.LoadImportFile(it)) } }

    LaunchedEffect(uiState.dummyDataResult) {
        uiState.dummyDataResult?.let { result ->
            val message = when (result) {
                is DummyDataResult.Success -> context.getString(R.string.dummy_data_success)
                is DummyDataResult.Failure -> context.getString(R.string.dummy_data_failure, result.cause ?: "")
            }
            snackbarHostState.showSnackbar(message)
            onIntent(SettingsIntent.ClearDummyDataResult)
        }
    }

    LaunchedEffect(uiState.deleteDataResult) {
        uiState.deleteDataResult?.let { result ->
            val message = when (result) {
                is DummyDataResult.Success -> context.getString(R.string.delete_data_success)
                is DummyDataResult.Failure -> context.getString(R.string.dummy_data_failure, result.cause ?: "")
            }
            snackbarHostState.showSnackbar(message)
            onIntent(SettingsIntent.ClearDeleteDataResult)
        }
    }

    LaunchedEffect(uiState.backupResult) {
        uiState.backupResult?.let { result ->
            val message = when (result) {
                BackupResult.ExportSuccess -> context.getString(R.string.backup_export_success)
                BackupResult.ImportSuccess -> context.getString(R.string.backup_import_success)
                BackupResult.ImportInvalidFile -> context.getString(R.string.backup_import_invalid)
                is BackupResult.Failure -> context.getString(R.string.backup_error, result.cause ?: "")
            }
            snackbarHostState.showSnackbar(message)
            onIntent(SettingsIntent.ClearBackupResult)
        }
    }

    // Import confirmation dialog
    if (uiState.showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.CancelImport) },
            title = {
                Text(
                    text = stringResource(R.string.backup_import_confirm_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.backup_import_confirm_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.ConfirmImport) }) {
                    Text(
                        text = stringResource(R.string.backup_import_confirm_action),
                        color = PositiveGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.CancelImport) }) {
                    Text(
                        text = stringResource(R.string.btn_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Text(
                text = stringResource(R.string.title_settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                ProfileCard(
                    name = uiState.userProfile.name.ifBlank { "User" },
                    since = uiState.trackingSince,
                    snapshots = uiState.snapshotCount,
                    onEditClick = { onIntent(SettingsIntent.NavigateToProfileEdit) }
                )
            }

            item { SettingsSectionHeader(title = stringResource(R.string.header_preferences)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        PreferenceRow(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.label_language),
                            selectedOption = when (uiState.appLanguage) {
                                AppLanguage.ENGLISH -> 0
                                AppLanguage.GREEK -> 1
                            },
                            options = listOf(
                                stringResource(R.string.language_english),
                                stringResource(R.string.language_greek)
                            ),
                            onOptionSelected = {
                                val lang = if (it == 0) AppLanguage.ENGLISH else AppLanguage.GREEK
                                onIntent(SettingsIntent.SetLanguage(lang))
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        PreferenceRow(
                            icon = Icons.Default.Settings,
                            title = stringResource(R.string.label_theme),
                            selectedOption = when (uiState.appTheme) {
                                AppTheme.LIGHT -> 0
                                AppTheme.DARK -> 1
                                AppTheme.SYSTEM -> 2
                            },
                            options = listOf(
                                stringResource(R.string.theme_light),
                                stringResource(R.string.theme_dark),
                                stringResource(R.string.theme_auto)
                            ),
                            onOptionSelected = {
                                val theme = when (it) {
                                    0 -> AppTheme.LIGHT
                                    1 -> AppTheme.DARK
                                    else -> AppTheme.SYSTEM
                                }
                                onIntent(SettingsIntent.SetTheme(theme))
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        PreferenceRow(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.label_currency),
                            selectedOption = when (uiState.appCurrency) {
                                AppCurrency.EUR -> 0
                                AppCurrency.USD -> 1
                                AppCurrency.GBP -> 2
                            },
                            options = listOf(
                                stringResource(R.string.currency_eur),
                                stringResource(R.string.currency_usd),
                                stringResource(R.string.currency_gbp)
                            ),
                            onOptionSelected = {
                                val currency = when (it) {
                                    0 -> AppCurrency.EUR
                                    1 -> AppCurrency.USD
                                    else -> AppCurrency.GBP
                                }
                                onIntent(SettingsIntent.SetCurrency(currency))
                            }
                        )
                    }
                }
            }

            item { SettingsSectionHeader(title = stringResource(R.string.header_finance)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    NavigationRow(
                        icon = "💸",
                        title = stringResource(R.string.fixed_expense_settings_title),
                        description = stringResource(R.string.fixed_expense_settings_subtitle),
                        trailingText = uiState.fixedExpensesYearlySummary.ifBlank { null },
                        onClick = { onIntent(SettingsIntent.NavigateToFixedExpenses) },
                    )
                }
            }

            item { SettingsSectionHeader(title = stringResource(R.string.header_smart_features)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        SmartFeatureRow(icon = Icons.Default.Info, title = stringResource(R.string.label_forecasts), description = stringResource(R.string.desc_forecasts), isChecked = true)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        SmartFeatureRow(icon = Icons.Default.Notifications, title = stringResource(R.string.label_monthly_reminder), description = stringResource(R.string.desc_monthly_reminder), isChecked = true)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        SmartFeatureRow(
                            icon = Icons.Default.Lock,
                            title = stringResource(R.string.label_app_security),
                            description = stringResource(R.string.desc_app_security),
                            isChecked = uiState.isSecurityEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) onIntent(SettingsIntent.NavigateToSetupPin)
                                else onIntent(SettingsIntent.DisableSecurity)
                            }
                        )
                    }
                }
            }

            item { SettingsSectionHeader(title = stringResource(R.string.header_about)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIntent(SettingsIntent.NavigateToOnboarding) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.label_view_onboarding), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = stringResource(R.string.desc_view_onboarding), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Backup & Restore section ─────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionHeader(title = stringResource(R.string.backup_section_title))
            }

            item {
                BackupRestoreSection(
                    isExporting    = uiState.isExporting,
                    isImporting    = uiState.isImporting,
                    isExportingCsv = uiState.isExportingCsv,
                    onExportClick = {
                        val today = LocalDate.now().toString()
                        exportLauncher.launch("networth_backup_$today.json")
                    },
                    onExportCsvClick = {
                        val today = LocalDate.now().toString()
                        exportCsvLauncher.launch("networth_$today.csv")
                    },
                    onImportClick = {
                        importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                    },
                )
            }

            // ── Developer section ────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionHeader(title = stringResource(R.string.settings_section_developer))
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onIntent(SettingsIntent.GenerateSpecificData) },
                        enabled = !uiState.isGeneratingSpecific && !uiState.isDummyDataGenerating && !uiState.isDeleting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (uiState.isGeneratingSpecific) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_generating))
                        } else {
                            Text(stringResource(R.string.btn_generate_specific_data))
                        }
                    }
                    Button(
                        onClick = { onIntent(SettingsIntent.GenerateDummyData) },
                        enabled = !uiState.isDummyDataGenerating && !uiState.isDeleting && !uiState.isGeneratingSpecific,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (uiState.isDummyDataGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_generating))
                        } else {
                            Text(stringResource(R.string.btn_generate_data))
                        }
                    }
                    Button(
                        onClick = { onIntent(SettingsIntent.DeleteData) },
                        enabled = !uiState.isDeleting && !uiState.isDummyDataGenerating,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (uiState.isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_deleting))
                        } else {
                            Text(stringResource(R.string.btn_delete_data))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(name: String, since: String, snapshots: Int, onEditClick: () -> Unit) {
    val colors = com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp).height(110.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = colors.backgroundCard)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFF76C893)), contentAlignment = Alignment.Center) {
                Text(text = name.take(1).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = colors.contentPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = stringResource(R.string.label_tracking_since, since, snapshots), color = colors.contentSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onEditClick,
                colors = ButtonDefaults.buttonColors(containerColor = colors.backgroundSecondary),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            ) {
                Text(text = stringResource(R.string.btn_edit), color = colors.contentPrimary)
            }
        }
    }
}

@Composable
private fun PreferenceRow(icon: ImageVector, title: String, selectedOption: Int, options: List<String>, onOptionSelected: (Int) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(12.dp))
        SegmentedControl(options = options, selectedOption = selectedOption, onOptionSelected = onOptionSelected)
    }
}

@Composable
private fun SegmentedControl(options: List<String>, selectedOption: Int, onOptionSelected: (Int) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedOption
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize().padding(4.dp).clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent)
                        .clickable { onOptionSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartFeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    var localChecked by remember { mutableStateOf(isChecked) }
    val checked = if (onCheckedChange != null) isChecked else localChecked
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = { newValue ->
                if (onCheckedChange != null) onCheckedChange(newValue)
                else localChecked = newValue
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = PositiveGreen,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f),
                uncheckedBorderColor = Color.Transparent,
            )
        )
    }
}

@Composable
private fun NavigationRow(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = icon, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = PositiveGreen,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    NetWorthTheme {
        SettingsContent(
            uiState = SettingsUiState(appTheme = AppTheme.SYSTEM, appLanguage = AppLanguage.ENGLISH),
            onIntent = {}
        )
    }
}
