package com.agcoding.networkapp.home.presentation.mapper

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import com.agcoding.networkapp.shared.ui.model.EntryUiModel
import com.agcoding.networkapp.shared.ui.model.GroupedEntries
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class NetWorthEntryToUiMapper @Inject constructor() {

    fun mapToUiModel(entry: NetWorthEntry): EntryUiModel = EntryUiModel(
        id = entry.id,
        formattedDate = entry.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())),
        formattedValue = "€${String.format(Locale.US, "%,.0f", entry.value)}"
    )

    fun groupByMonth(entries: List<NetWorthEntry>): List<GroupedEntries> =
        entries
            .sortedByDescending { it.date }
            .groupBy { YearMonth.from(it.date) }
            .entries
            .sortedByDescending { it.key }
            .map { (yearMonth, monthEntries) ->
                GroupedEntries(
                    monthHeader = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    entries = monthEntries.map { mapToUiModel(it) }
                )
            }
}
