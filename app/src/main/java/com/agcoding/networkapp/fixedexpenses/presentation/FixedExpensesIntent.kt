package com.agcoding.networkapp.fixedexpenses.presentation

import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.presentation.model.FixedExpenseUiModel
import java.time.LocalDate

sealed interface FixedExpensesIntent {
    data object ShowAddSheet : FixedExpensesIntent
    data class ShowEditSheet(val expense: FixedExpenseUiModel) : FixedExpensesIntent
    data object HideSheet : FixedExpensesIntent
    data class UpdateTitle(val value: String) : FixedExpensesIntent
    data class UpdateNote(val value: String) : FixedExpensesIntent
    data class UpdateCost(val value: String) : FixedExpensesIntent
    data class UpdateDate(val date: LocalDate?) : FixedExpensesIntent
    data class UpdateRecurrence(val recurrence: RecurrenceType) : FixedExpensesIntent
    data object Save : FixedExpensesIntent
    data class Delete(val id: Long) : FixedExpensesIntent
    data object ClearError : FixedExpensesIntent
}
