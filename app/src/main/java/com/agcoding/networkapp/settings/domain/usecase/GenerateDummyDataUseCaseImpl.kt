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
import java.time.YearMonth
import javax.inject.Inject

class GenerateDummyDataUseCaseImpl @Inject constructor(
    private val netWorthRepository: NetWorthRepository,
    private val accountRepository: AccountRepository,
    private val fixedExpensesRepository: FixedExpensesRepository,
) : GenerateDummyDataUseCase {

    override suspend operator fun invoke(): Result<Unit> {
        netWorthRepository.deleteAllEntries().getOrElse { return Result.failure(it) }
        fixedExpensesRepository.deleteAll().getOrElse { return Result.failure(it) }

        // Reset to single Main account
        val existing = accountRepository.getAccounts().first()
        existing.filter { it.id != 1L }.forEach { accountRepository.deleteAccount(it.id) }

        val savingsId = accountRepository.createAccount(
            Account(name = "Savings", startingBalance = 0.0, colorHex = "#5B8DEF")
        )
        val investmentsId = accountRepository.createAccount(
            Account(name = "Investments", startingBalance = 0.0, colorHex = "#A78BFA")
        )
        val cashId = accountRepository.createAccount(
            Account(name = "Cash", startingBalance = 0.0, colorHex = "#F59E0B")
        )

        // Net-worth entries — 36 months
        val now = YearMonth.now()
        suspend fun addEntries(values: List<Double>, accountId: Long) {
            values.forEachIndexed { index, value ->
                val month = now.minusMonths((values.size - 1 - index).toLong())
                netWorthRepository.addEntry(NetWorthEntry(value = value, date = month.atDay(15), accountId = accountId))
            }
        }

        addEntries(mainAccountValues,        accountId = 1L)
        addEntries(savingsValues,            accountId = savingsId)
        addEntries(investmentsValues,        accountId = investmentsId)
        addEntries(cashValues,               accountId = cashId)

        // Fixed expenses — varied accounts, monthly & annual
        val main        = listOf(1L)
        val savings     = listOf(savingsId)
        val investments = listOf(investmentsId)
        val mainSavings = listOf(1L, savingsId)
        val allAccounts = emptyList<Long>() // empty = belongs to all

        val baseDate = LocalDate.now().withDayOfMonth(1)

        val expenses = listOf(
            // Housing & Utilities — Main
            FixedExpense(title = "Rent",              cost = 950.0,   recurrence = RecurrenceType.MONTHLY, accountIds = main,        note = "Apartment", date = baseDate),
            FixedExpense(title = "Electric Bill",     cost = 95.0,    recurrence = RecurrenceType.MONTHLY, accountIds = mainSavings,  note = "Shared",    date = baseDate),
            FixedExpense(title = "Water & Heating",   cost = 55.0,    recurrence = RecurrenceType.MONTHLY, accountIds = mainSavings,  note = "Shared"),
            FixedExpense(title = "Internet",          cost = 44.99,   recurrence = RecurrenceType.MONTHLY, accountIds = main),
            FixedExpense(title = "Phone Plan",        cost = 34.99,   recurrence = RecurrenceType.MONTHLY, accountIds = main),

            // Insurance — Main, annual
            FixedExpense(title = "Health Insurance",  cost = 1_980.0, recurrence = RecurrenceType.ANNUAL,  accountIds = main,        note = "Private",   date = baseDate.withMonth(1)),
            FixedExpense(title = "Car Insurance",     cost = 840.0,   recurrence = RecurrenceType.ANNUAL,  accountIds = main,        note = "Full cover", date = baseDate.withMonth(3)),
            FixedExpense(title = "Home Insurance",    cost = 360.0,   recurrence = RecurrenceType.ANNUAL,  accountIds = main,        date = baseDate.withMonth(6)),

            // Fitness & Lifestyle — Main
            FixedExpense(title = "Gym Membership",   cost = 49.0,    recurrence = RecurrenceType.MONTHLY, accountIds = main),
            FixedExpense(title = "Parking Spot",     cost = 80.0,    recurrence = RecurrenceType.MONTHLY, accountIds = main,        note = "Monthly permit"),

            // Streaming — mixed accounts
            FixedExpense(title = "Netflix",          cost = 17.99,   recurrence = RecurrenceType.MONTHLY, accountIds = mainSavings,  note = "Family plan"),
            FixedExpense(title = "Spotify",          cost = 10.99,   recurrence = RecurrenceType.MONTHLY, accountIds = savings),
            FixedExpense(title = "YouTube Premium",  cost = 13.99,   recurrence = RecurrenceType.MONTHLY, accountIds = allAccounts,  note = "Family"),
            FixedExpense(title = "Audible",          cost = 14.99,   recurrence = RecurrenceType.MONTHLY, accountIds = savings),
            FixedExpense(title = "Apple TV+",        cost = 8.99,    recurrence = RecurrenceType.MONTHLY, accountIds = savings),

            // Software & Cloud — Investments
            FixedExpense(title = "Adobe Creative",   cost = 54.99,   recurrence = RecurrenceType.MONTHLY, accountIds = investments,  note = "All Apps"),
            FixedExpense(title = "GitHub Pro",       cost = 48.0,    recurrence = RecurrenceType.ANNUAL,  accountIds = investments),
            FixedExpense(title = "Hosting & Domain", cost = 199.0,   recurrence = RecurrenceType.ANNUAL,  accountIds = investments,  note = "VPS + 2 domains"),
            FixedExpense(title = "VPN Service",      cost = 59.88,   recurrence = RecurrenceType.ANNUAL,  accountIds = investments),
            FixedExpense(title = "Cloud Backup",     cost = 119.88,  recurrence = RecurrenceType.ANNUAL,  accountIds = investments,  note = "Backblaze"),

            // Miscellaneous — all or cash
            FixedExpense(title = "iCloud Storage",   cost = 2.99,    recurrence = RecurrenceType.MONTHLY, accountIds = allAccounts),
            FixedExpense(title = "News Subscription", cost = 9.99,   recurrence = RecurrenceType.MONTHLY, accountIds = listOf(cashId), note = "Digital"),
            FixedExpense(title = "Coffee Subscription", cost = 24.0, recurrence = RecurrenceType.MONTHLY, accountIds = listOf(cashId), note = "Beans monthly"),
        )

        expenses.forEach { expense ->
            fixedExpensesRepository.add(expense).getOrElse { return Result.failure(it) }
        }

        return Result.success(Unit)
    }

    companion object {
        // 36 months — Main account (steady growth with minor dips)
        private val mainAccountValues = listOf(
             5_800.0,  6_200.0,  6_500.0,  6_800.0,  7_100.0,  7_400.0,
             7_800.0,  8_200.0,  8_700.0,  8_400.0,  9_100.0,  9_600.0,
            10_200.0, 10_500.0, 10_100.0, 10_800.0, 11_400.0, 11_900.0,
            12_500.0, 13_100.0, 12_800.0, 13_500.0, 14_200.0, 14_600.0,
            15_300.0, 16_000.0, 15_700.0, 16_500.0, 17_300.0, 18_100.0,
            19_000.0, 20_100.0, 21_200.0, 22_400.0, 23_700.0, 25_100.0,
        )

        // 36 months — Savings account (consistent monthly saving)
        private val savingsValues = listOf(
             1_200.0,  1_500.0,  1_800.0,  2_100.0,  2_400.0,  2_700.0,
             3_000.0,  3_400.0,  3_800.0,  4_200.0,  4_600.0,  5_100.0,
             5_600.0,  6_100.0,  6_700.0,  7_300.0,  7_900.0,  8_600.0,
             9_300.0, 10_100.0, 10_900.0, 11_800.0, 12_700.0, 13_700.0,
            14_700.0, 15_800.0, 16_900.0, 18_100.0, 19_400.0, 20_700.0,
            22_100.0, 23_600.0, 25_200.0, 26_900.0, 28_700.0, 30_600.0,
        )

        // 36 months — Investments (higher volatility, strong long-term growth)
        private val investmentsValues = listOf(
             1_500.0,  1_800.0,  2_200.0,  1_900.0,  2_500.0,  3_100.0,
             3_400.0,  2_900.0,  3_800.0,  4_200.0,  3_700.0,  4_500.0,
             5_200.0,  4_800.0,  5_600.0,  6_500.0,  7_200.0,  6_800.0,
             7_500.0,  8_400.0,  9_200.0, 10_100.0, 11_300.0, 12_600.0,
            11_800.0, 13_200.0, 14_700.0, 13_500.0, 15_200.0, 16_800.0,
            18_500.0, 17_200.0, 19_500.0, 21_000.0, 23_200.0, 25_800.0,
        )

        // 36 months — Cash (slowly accumulated, occasional spikes for bonuses)
        private val cashValues = listOf(
               400.0,    450.0,    500.0,    480.0,    520.0,    600.0,
               650.0,    700.0,    680.0,    750.0,    800.0,  1_200.0,
             1_100.0,  1_050.0,  1_150.0,  1_200.0,  1_300.0,  1_250.0,
             1_400.0,  1_500.0,  1_450.0,  1_600.0,  1_700.0,  2_100.0,
             2_000.0,  1_950.0,  2_100.0,  2_200.0,  2_350.0,  2_300.0,
             2_500.0,  2_600.0,  2_750.0,  2_900.0,  3_050.0,  3_200.0,
        )
    }
}
