package com.agcoding.networkapp.fixedexpenses.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agcoding.networkapp.R
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.presentation.FixedExpensesUiState
import com.agcoding.networkapp.home.presentation.components.EntryDatePickerDialog
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedExpenseBottomSheet(
    uiState: FixedExpensesUiState,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onDateChange: (LocalDate?) -> Unit,
    onRecurrenceChange: (RecurrenceType) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()) }
    val isEditing = uiState.editingExpense != null
    val canSave = uiState.titleInput.isNotBlank() && uiState.costInput.toDoubleOrNull() != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isEditing) stringResource(R.string.fixed_expense_sheet_edit_title)
                       else stringResource(R.string.fixed_expense_sheet_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(4.dp))

            // Title
            OutlinedTextField(
                value = uiState.titleInput,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.fixed_expense_hint_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Cost
            OutlinedTextField(
                value = uiState.costInput,
                onValueChange = onCostChange,
                label = { Text(stringResource(R.string.fixed_expense_hint_cost)) },
                singleLine = true,
                prefix = { Text(uiState.currencySymbol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Recurrence chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RecurrenceChip(
                    label = stringResource(R.string.fixed_expense_recurrence_monthly),
                    isSelected = uiState.recurrenceInput == RecurrenceType.MONTHLY,
                    onClick = { onRecurrenceChange(RecurrenceType.MONTHLY) },
                    modifier = Modifier.weight(1f),
                )
                RecurrenceChip(
                    label = stringResource(R.string.fixed_expense_recurrence_annual),
                    isSelected = uiState.recurrenceInput == RecurrenceType.ANNUAL,
                    onClick = { onRecurrenceChange(RecurrenceType.ANNUAL) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Note + Date side-by-side (both optional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = uiState.noteInput,
                    onValueChange = onNoteChange,
                    label = { Text(stringResource(R.string.fixed_expense_hint_note)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                )

                // Date — clickable field that opens picker
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = uiState.dateInput?.format(dateFormatter) ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text(stringResource(R.string.fixed_expense_hint_date)) },
                        trailingIcon = {
                            if (uiState.dateInput != null) {
                                IconButton(onClick = { onDateChange(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                    // Transparent overlay — captures tap and opens date picker
                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onSave,
                enabled = canSave && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
            ) {
                Text(
                    text = stringResource(R.string.fixed_expense_btn_save),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (isEditing) {
                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.fixed_expense_btn_delete),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        EntryDatePickerDialog(
            selectedDate = uiState.dateInput ?: LocalDate.now(),
            onDateSelected = { date ->
                onDateChange(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun RecurrenceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ) else null,
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FixedExpenseBottomSheetPreview() {
    NetWorthTheme {
        FixedExpenseBottomSheet(
            uiState = FixedExpensesUiState(isSheetVisible = true, currencySymbol = "€"),
            onTitleChange = {},
            onNoteChange = {},
            onCostChange = {},
            onDateChange = {},
            onRecurrenceChange = {},
            onSave = {},
            onDelete = {},
            onDismiss = {},
        )
    }
}
