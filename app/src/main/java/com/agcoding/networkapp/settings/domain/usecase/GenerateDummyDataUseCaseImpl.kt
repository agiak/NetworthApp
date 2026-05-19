package com.agcoding.networkapp.settings.domain.usecase

import com.agcoding.networkapp.account.domain.model.Account
import com.agcoding.networkapp.account.domain.repository.AccountRepository
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import javax.inject.Inject

class GenerateDummyDataUseCaseImpl @Inject constructor(
    private val netWorthRepository: NetWorthRepository,
    private val accountRepository: AccountRepository,
) : GenerateDummyDataUseCase {

    override suspend operator fun invoke(): Result<Unit> {
        netWorthRepository.deleteAllEntries().getOrElse { return Result.failure(it) }

        // Reset to single Main account: delete any extra accounts
        val existing = accountRepository.getAccounts().first()
        existing.filter { it.id != 1L }.forEach { accountRepository.deleteAccount(it.id) }

        // Create Savings and Investments accounts
        val savingsId = accountRepository.createAccount(
            Account(name = "Savings", startingBalance = 0.0, colorHex = "#5B8DEF")
        )
        val investmentsId = accountRepository.createAccount(
            Account(name = "Investments", startingBalance = 0.0, colorHex = "#A78BFA")
        )

        val now = YearMonth.now()
        suspend fun addEntries(values: List<Double>, accountId: Long) {
            values.forEachIndexed { index, value ->
                val month = now.minusMonths((values.size - 1 - index).toLong())
                val entry = NetWorthEntry(value = value, date = month.atDay(15), accountId = accountId)
                netWorthRepository.addEntry(entry)
            }
        }

        addEntries(mainAccountValues, accountId = 1L)
        addEntries(savingsValues,     accountId = savingsId)
        addEntries(investmentsValues, accountId = investmentsId)

        return Result.success(Unit)
    }

    companion object {
        // 36 months — Main account (steady overall growth)
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

        // 36 months — Investments account (higher volatility)
        private val investmentsValues = listOf(
             1_500.0,  1_800.0,  2_200.0,  1_900.0,  2_500.0,  3_100.0,
             3_400.0,  2_900.0,  3_800.0,  4_200.0,  3_700.0,  4_500.0,
             5_200.0,  4_800.0,  5_600.0,  6_500.0,  7_200.0,  6_800.0,
             7_500.0,  8_400.0,  9_200.0, 10_100.0, 11_300.0, 12_600.0,
            11_800.0, 13_200.0, 14_700.0, 13_500.0, 15_200.0, 16_800.0,
            18_500.0, 17_200.0, 19_500.0, 21_000.0, 23_200.0, 25_800.0,
        )
    }
}
