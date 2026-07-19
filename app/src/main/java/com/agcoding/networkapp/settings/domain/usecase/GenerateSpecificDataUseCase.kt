package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.repository.AccountRepository
import com.agcoding.networkapp.fixedexpenses.domain.model.FixedExpense
import com.agcoding.networkapp.fixedexpenses.domain.model.RecurrenceType
import com.agcoding.networkapp.fixedexpenses.domain.repository.FixedExpensesRepository
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class GenerateSpecificDataUseCase @Inject constructor(
    private val netWorthRepository: NetWorthRepository,
    private val accountRepository: AccountRepository,
    private val fixedExpensesRepository: FixedExpensesRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        netWorthRepository.deleteAllEntries().getOrElse { return Result.failure(it) }
        fixedExpensesRepository.deleteAll().getOrElse { return Result.failure(it) }

        // Reset to the two real accounts
        val existing = accountRepository.getAccounts().first()
        existing.filter { it.id != 1L }.forEach { accountRepository.deleteAccount(it.id) }
        accountRepository.updateAccount(
            Account(id = 1L, name = "Anastasis", startingBalance = 0.0, colorHex = "#76C893")
        )
        val xristinaId = accountRepository.createAccount(
            Account(name = "Xristina", startingBalance = 42_463.0, colorHex = "#A78BFA")
        )

        val anastasisEntries = listOf(
            NetWorthEntry(value = 36_029.0, date = LocalDate.of(2024, 10, 25)),
            NetWorthEntry(value = 38_329.0, date = LocalDate.of(2024, 11, 25)),
            NetWorthEntry(value = 39_149.0, date = LocalDate.of(2024, 12, 25)),
            NetWorthEntry(value = 42_265.0, date = LocalDate.of(2025, 1, 25)),
            NetWorthEntry(value = 43_103.0, date = LocalDate.of(2025, 2, 25)),
            NetWorthEntry(value = 43_510.0, date = LocalDate.of(2025, 3, 25)),
            NetWorthEntry(value = 41_989.0, date = LocalDate.of(2025, 4, 25)),
            NetWorthEntry(value = 42_855.0, date = LocalDate.of(2025, 5, 25)),
            NetWorthEntry(value = 44_743.0, date = LocalDate.of(2025, 6, 25)),
            NetWorthEntry(value = 37_992.0, date = LocalDate.of(2025, 7, 25)),
            NetWorthEntry(value = 39_981.0, date = LocalDate.of(2025, 8, 25)),
            NetWorthEntry(value = 41_570.0, date = LocalDate.of(2025, 9, 25)),
            // No entry for October 2025
            NetWorthEntry(value = 46_237.0, date = LocalDate.of(2025, 11, 25)),
            NetWorthEntry(value = 53_541.0, date = LocalDate.of(2025, 12, 25)),
            NetWorthEntry(value = 57_749.0, date = LocalDate.of(2026, 1, 25)),
            NetWorthEntry(value = 62_246.0, date = LocalDate.of(2026, 2, 25)),
            NetWorthEntry(value = 60_982.0, date = LocalDate.of(2026, 3, 25)),
            NetWorthEntry(value = 65_855.0, date = LocalDate.of(2026, 4, 25)),
            NetWorthEntry(value = 71_041.0, date = LocalDate.of(2026, 5, 25)),
            NetWorthEntry(value = 73_826.0, date = LocalDate.of(2026, 6, 25)),
        )
        val xristinaEntries = listOf(
            NetWorthEntry(value = 42_463.0, date = LocalDate.of(2026, 6, 3), accountId = xristinaId),
            NetWorthEntry(value = 44_824.0, date = LocalDate.of(2026, 6, 25), accountId = xristinaId),
        )
        (anastasisEntries + xristinaEntries).forEach { entry ->
            netWorthRepository.addEntry(entry).getOrElse { return Result.failure(it) }
        }

        val anastasis = listOf(1L)
        val allAccounts = emptyList<Long>() // empty = belongs to all
        val expenses = listOf(
            FixedExpense(title = "House",           cost = 350.0, recurrence = RecurrenceType.MONTHLY, accountIds = allAccounts),
            FixedExpense(title = "Claude",          cost = 18.0,  recurrence = RecurrenceType.MONTHLY, accountIds = anastasis),
            FixedExpense(title = "Parking",         cost = 145.0, recurrence = RecurrenceType.MONTHLY, accountIds = allAccounts),
            FixedExpense(title = "Google One",      cost = 22.0,  recurrence = RecurrenceType.ANNUAL,  accountIds = anastasis),
            FixedExpense(title = "Car insurance",   cost = 600.0, recurrence = RecurrenceType.ANNUAL,  accountIds = allAccounts),
            FixedExpense(title = "Cell phone bill", cost = 12.0,  recurrence = RecurrenceType.MONTHLY, accountIds = anastasis),
            FixedExpense(title = "Internet",        cost = 24.0,  recurrence = RecurrenceType.MONTHLY, accountIds = allAccounts),
            FixedExpense(title = "Electricity",     cost = 75.0,  recurrence = RecurrenceType.MONTHLY, accountIds = allAccounts),
            FixedExpense(title = "Gym",             cost = 580.0, recurrence = RecurrenceType.ANNUAL,  accountIds = anastasis),
            FixedExpense(title = "Revolut plus",    cost = 4.0,   recurrence = RecurrenceType.MONTHLY, accountIds = anastasis),
            FixedExpense(title = "Bank fees",       cost = 8.5,   recurrence = RecurrenceType.MONTHLY, accountIds = anastasis),
        )
        expenses.forEach { expense ->
            fixedExpensesRepository.add(expense).getOrElse { return Result.failure(it) }
        }

        return Result.success(Unit)
    }
}
