package com.agcoding.networkapp.home.presentation.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.agcoding.networkapp.R
import com.agcoding.networkapp.shared.ui.theme.NetWorthTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 86400000L
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onDateSelected(LocalDate.ofEpochDay(millis / 86400000L))
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.btn_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryDatePickerDialogPreview() {
    NetWorthTheme {
        EntryDatePickerDialog(selectedDate = LocalDate.now(), onDateSelected = {}, onDismiss = {})
    }
}
