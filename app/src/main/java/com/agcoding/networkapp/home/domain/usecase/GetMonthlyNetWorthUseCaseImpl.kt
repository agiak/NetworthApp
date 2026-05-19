package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.home.domain.repository.NetWorthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

class GetMonthlyNetWorthUseCaseImpl @Inject constructor(
    private val repository: NetWorthRepository
) : GetMonthlyNetWorthUseCase {

    override operator fun invoke(): Flow<Result<List<MonthlyNetWorth>>> =
        repository.getEntries().map { result -> result.map { aggregateToMonthly(it) } }

    private fun aggregateToMonthly(entries: List<NetWorthEntry>): List<MonthlyNetWorth> {
        if (entries.isEmpty()) return emptyList()

        val accountIds = entries.map { it.accountId }.distinct()
        val entriesByAccount = entries.groupBy { it.accountId }

        val firstMonth = entries.map { YearMonth.from(it.date) }.min()
        val lastMonth  = entries.map { YearMonth.from(it.date) }.max()

        val result = mutableListOf<MonthlyNetWorth>()
        // Running balance per account — carries forward until updated
        val accountCurrentValue = mutableMapOf<Long, Double>()
        var current = firstMonth

        while (!current.isAfter(lastMonth)) {
            var hasNewEntry = false
            var latestDate = current.atDay(1)

            for (accountId in accountIds) {
                val monthEntry = entriesByAccount[accountId]
                    ?.filter { YearMonth.from(it.date) == current }
                    ?.maxByOrNull { it.date.toEpochDay() * 100_000 + it.id }

                if (monthEntry != null) {
                    accountCurrentValue[accountId] = monthEntry.value
                    hasNewEntry = true
                    if (monthEntry.date > latestDate) latestDate = monthEntry.date
                }
                // If no entry this month, the running value carries forward automatically
            }

            if (accountCurrentValue.isNotEmpty()) {
                result.add(
                    MonthlyNetWorth(
                        yearMonth       = current,
                        value           = accountCurrentValue.values.sum(),
                        lastUpdatedDate = latestDate,
                        isCarriedForward = !hasNewEntry,
                    )
                )
            }
            current = current.plusMonths(1)
        }

        return result
    }
}
