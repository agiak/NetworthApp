package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import java.time.YearMonth

fun computeMonthlyForAccount(entries: List<NetWorthEntry>, accountId: Long): List<MonthlyNetWorth> {
    val accountEntries = entries.filter { it.accountId == accountId }
    if (accountEntries.isEmpty()) return emptyList()

    val firstMonth = accountEntries.map { YearMonth.from(it.date) }.min()
    val lastMonth  = accountEntries.map { YearMonth.from(it.date) }.max()
    val byMonth    = accountEntries.groupBy { YearMonth.from(it.date) }

    var lastValue = byMonth[firstMonth]!!.maxByOrNull { it.date }!!.value
    val result    = mutableListOf<MonthlyNetWorth>()
    var current   = firstMonth

    while (!current.isAfter(lastMonth)) {
        val monthEntries = byMonth[current]
        val isCarried    = monthEntries == null
        if (!isCarried) lastValue = monthEntries!!.maxByOrNull { it.date }!!.value
        result.add(
            MonthlyNetWorth(
                yearMonth        = current,
                value            = lastValue,
                lastUpdatedDate  = monthEntries?.maxByOrNull { it.date }?.date ?: current.atDay(15),
                isCarriedForward = isCarried,
            )
        )
        current = current.plusMonths(1)
    }
    return result
}
