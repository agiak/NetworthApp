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

        val byMonth = entries
            .groupBy { YearMonth.from(it.date) }
            .mapValues { (_, monthEntries) -> 
                monthEntries.sortedWith(compareBy({ it.date }, { it.id })).last() 
            }

        val sortedMonths = byMonth.keys.sorted()
        val result = mutableListOf<MonthlyNetWorth>()
        var prevValue: Double? = null
        var current = sortedMonths.first()
        val last = sortedMonths.last()

        while (!current.isAfter(last)) {
            val entry = byMonth[current]
            if (entry != null) {
                result.add(MonthlyNetWorth(yearMonth = current, value = entry.value, lastUpdatedDate = entry.date))
                prevValue = entry.value
            } else if (prevValue != null) {
                result.add(
                    MonthlyNetWorth(
                        yearMonth = current,
                        value = prevValue!!,
                        lastUpdatedDate = current.atEndOfMonth(),
                        isCarriedForward = true
                    )
                )
            }
            current = current.plusMonths(1)
        }

        return result
    }
}
