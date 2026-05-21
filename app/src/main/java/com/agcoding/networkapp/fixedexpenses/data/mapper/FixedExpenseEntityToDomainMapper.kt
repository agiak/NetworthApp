package com.agcoding.networkapp.fixedexpenses.data.mapper

import com.agcoding.networkapp.fixedexpenses.data.local.FixedExpenseEntity
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import java.time.LocalDate
import javax.inject.Inject

class FixedExpenseEntityToDomainMapper @Inject constructor() {

    fun toDomain(entity: FixedExpenseEntity) = FixedExpense(
        id = entity.id,
        title = entity.title,
        note = entity.note,
        cost = entity.cost,
        date = entity.dateEpochDay?.let { LocalDate.ofEpochDay(it) },
        recurrence = RecurrenceType.entries.firstOrNull { it.name == entity.recurrence }
            ?: RecurrenceType.MONTHLY,
        accountIds = entity.accountIds.toAccountIdList(),
    )

    fun toEntity(domain: FixedExpense) = FixedExpenseEntity(
        id = domain.id,
        title = domain.title,
        note = domain.note,
        cost = domain.cost,
        dateEpochDay = domain.date?.toEpochDay(),
        recurrence = domain.recurrence.name,
        accountIds = domain.accountIds.joinToString(","),
    )

    private fun String.toAccountIdList(): List<Long> =
        if (isBlank()) emptyList()
        else split(",").mapNotNull { it.trim().toLongOrNull() }
}
