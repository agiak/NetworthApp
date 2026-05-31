package com.agcoding.networkapp.settings.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.LocalAppColorScheme
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import com.agcoding.networkapp.shared.ui.utils.formatWithSeparator

@Composable
fun ProfileTargetSetupScreen(
    onComplete: () -> Unit,
    viewModel: ProfileTargetSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onComplete()
    }

    ProfileTargetSetupContent(
        uiState = uiState,
        onTargetChange = viewModel::onTargetChange,
        onSave = viewModel::onSave,
        onSkip = viewModel::onSkip,
    )
}

@Composable
private fun ProfileTargetSetupContent(
    uiState: ProfileTargetSetupUiState,
    onTargetChange: (String) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit = {},
) {
    val colors = LocalAppColorScheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.label_target_amount).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = colors.contentSecondary,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.title_setup_target),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = colors.contentPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.desc_setup_target),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.contentSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        // Large amount display
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "€",
                style = MaterialTheme.typography.displaySmall,
                color = colors.contentPrimary.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (uiState.target.isEmpty()) "0" else formatWithSeparator(uiState.target),
                style = MaterialTheme.typography.displayLarge,
                color = if (uiState.target.isEmpty()) colors.contentPrimary.copy(alpha = 0.3f)
                        else colors.contentPrimary,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(20.dp))

        // Quick preset chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("50000", "100000", "250000", "500000").forEach { preset ->
                TargetPresetChip(
                    label = formatPreset(preset),
                    isSelected = uiState.target == preset,
                    onClick = { onTargetChange(preset) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Numpad
        val keyRows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL"),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            keyRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            TargetKeypadButton(
                                label = key,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    when (key) {
                                        "DEL" -> if (uiState.target.isNotEmpty()) {
                                            onTargetChange(uiState.target.dropLast(1))
                                        }
                                        else -> if (uiState.target.length < 10) {
                                            onTargetChange(uiState.target + key)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.actionPrimary,
                contentColor = colors.actionContent,
                disabledContainerColor = colors.actionPrimary.copy(alpha = 0.38f),
                disabledContentColor = colors.actionContent.copy(alpha = 0.38f),
            ),
            enabled = uiState.target.isNotEmpty() && !uiState.isSaving,
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    color = colors.actionContent,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.btn_continue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
        ) {
            Text(
                text = stringResource(R.string.btn_skip),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.contentSecondary,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun TargetPresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColorScheme.current
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.actionPrimary else MaterialTheme.colorScheme.surface,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colors.actionContent else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TargetKeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    Surface(
        onClick = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.height(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (label == "DEL") {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun formatPreset(value: String): String {
    val n = value.toLongOrNull() ?: return value
    return when {
        n >= 1_000_000L -> "€${n / 1_000_000}M"
        n >= 1_000L     -> "€${n / 1_000}k"
        else            -> "€$n"
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileTargetSetupPreview() {
    NetWorthTheme {
        ProfileTargetSetupContent(
            uiState = ProfileTargetSetupUiState(target = "100000"),
            onTargetChange = {},
            onSave = {}
        )
    }
}
