package com.agcoding.networkapp.analytics.presentation.mapper

import com.agcoding.networkapp.analytics.presentation.TimeFilter
import com.agcoding.networkapp.analytics.presentation.model.MonthlyEntryUiModel
import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

data class AnalyticsMapResult(
    val chartData: List<ChartPoint> = emptyList(),
    val chartStartLabel: String = "",
    val chartMidLabel: String = "",
    val chartEndLabel: String = "",
    val totalGrowth: String = "",
    val totalGrowthPercent: String = "",
    val totalGrowthPositive: Boolean = true,
    val avgMonthlyGrowth: String = "",
    val bestMonthLabel: String = "",
    val bestMonthValue: String = "",
    val worstMonthLabel: String = "",
    val worstMonthValue: String = "",
    val highestNetWorth: String = "",
    val highestNetWorthDate: String = "",
    val lowestNetWorth: String = "",
    val lowestNetWorthDate: String = "",
    val trendLabel: String = "",
    val trendDescription: String = "",
    val trendIsPositive: Boolean = false,
    val trendIsNeutral: Boolean = true,
    val consistencyPercent: String = "",
    val consistencyDetail: String = "",
    val projectedNetWorth: String = "",
    val projectedNetWorthDate: String = "",
    val currentStreakLabel: String = "",
    val currentStreakSubLabel: String = "",
    val monthlyEntries: List<MonthlyEntryUiModel> = emptyList(),
    val hasData: Boolean = false
)

class AnalyticsUiMapper @Inject constructor() {

    private var symbol: String = "€"

    fun map(allData: List<MonthlyNetWorth>, filter: TimeFilter, currency: AppCurrency = AppCurrency.EUR): AnalyticsMapResult {
        symbol = currency.symbol
        val filtered = applyFilter(allData, filter)
        if (filtered.isEmpty()) return AnalyticsMapResult()

        val sorted = filtered.sortedBy { it.yearMonth }

        val minVal = sorted.minOf { it.value }
        val maxVal = sorted.maxOf { it.value }
        val range = if (maxVal == minVal) 1.0 else maxVal - minVal
        val chartData = sorted.mapIndexed { i, m ->
            ChartPoint(
                x = if (sorted.size > 1) i.toFloat() / (sorted.size - 1) else 0.5f,
                y = ((m.value - minVal) / range).toFloat().coerceIn(0f, 1f)
            )
        }

        val axisFormatter = DateTimeFormatter.ofPattern("MMM ''yy", Locale.getDefault())
        val shortFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
        val fullFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

        val firstVal = sorted.first().value
        val lastVal = sorted.last().value
        val totalDiff = lastVal - firstVal
        val totalPct = if (firstVal != 0.0) (totalDiff / firstVal) * 100.0 else 0.0
        val avgGrowth = if (sorted.size >= 2) totalDiff / (sorted.size - 1) else 0.0

        val pairDiffs = sorted.zipWithNext().map { (prev, curr) -> curr to (curr.value - prev.value) }
        val bestEntry = pairDiffs.maxByOrNull { it.second }
        val worstEntry = pairDiffs.minByOrNull { it.second }

        val highestEntry = sorted.maxByOrNull { it.value }
        val lowestEntry = sorted.minByOrNull { it.value }

        val monthlyDiffs = sorted.zipWithNext().map { (prev, curr) -> curr.value - prev.value }
        val positiveMonths = monthlyDiffs.count { it > 0 }
        val totalDiffMonths = monthlyDiffs.size

        val consistencyPercent = if (totalDiffMonths > 0) "$positiveMonths/$totalDiffMonths" else "—"
        val consistencyDetail = if (totalDiffMonths > 0) "months positive" else ""

        var streak = 0
        for (diff in monthlyDiffs.reversed()) {
            if (diff > 0) streak++ else break
        }
        val currentStreakLabel = if (streak > 0) "$streak" else "—"
        val currentStreakSubLabel = if (streak > 0) "months in a row" else "No positive streak"

        val trendDirection = when {
            sorted.size < 2 -> 0
            sorted.size < 4 -> when {
                lastVal > firstVal * 1.02 -> 1
                lastVal < firstVal * 0.98 -> -1
                else -> 0
            }
            else -> {
                val mid = sorted.size / 2
                val firstHalfAvg = sorted.take(mid).sumOf { it.value } / mid
                val secondHalf = sorted.drop(mid)
                val secondHalfAvg = secondHalf.sumOf { it.value } / secondHalf.size
                val changePct = if (firstHalfAvg != 0.0) (secondHalfAvg - firstHalfAvg) / firstHalfAvg * 100 else 0.0
                when {
                    changePct > 2.0 -> 1
                    changePct < -2.0 -> -1
                    else -> 0
                }
            }
        }
        val trendLabel = when (trendDirection) {
            1 -> "Increasing"
            -1 -> "Decreasing"
            else -> "Stable"
        }
        val trendDescription = when (trendDirection) {
            1 -> "Net worth trending upward in this period"
            -1 -> "Net worth declining in this period"
            else -> "Net worth relatively stable in this period"
        }

        val projectedValue = lastVal + (avgGrowth * 12)
        val projectedDate = YearMonth.now().plusMonths(12).format(shortFormatter)

        return AnalyticsMapResult(
            chartData = chartData,
            chartStartLabel = sorted.first().yearMonth.format(axisFormatter),
            chartMidLabel = sorted.getOrNull(sorted.size / 2)?.yearMonth?.format(axisFormatter) ?: "",
            chartEndLabel = sorted.last().yearMonth.format(axisFormatter),
            totalGrowth = formatChange(totalDiff),
            totalGrowthPercent = formatPercent(totalPct),
            totalGrowthPositive = totalDiff >= 0,
            avgMonthlyGrowth = formatChange(avgGrowth),
            bestMonthLabel = bestEntry?.first?.yearMonth?.format(shortFormatter) ?: "",
            bestMonthValue = bestEntry?.second?.let { formatChange(it) } ?: "",
            worstMonthLabel = worstEntry?.first?.yearMonth?.format(shortFormatter) ?: "",
            worstMonthValue = worstEntry?.second?.let { formatChange(it) } ?: "",
            highestNetWorth = highestEntry?.value?.let { formatCurrency(it) } ?: "",
            highestNetWorthDate = highestEntry?.yearMonth?.format(shortFormatter) ?: "",
            lowestNetWorth = lowestEntry?.value?.let { formatCurrency(it) } ?: "",
            lowestNetWorthDate = lowestEntry?.yearMonth?.format(shortFormatter) ?: "",
            trendLabel = trendLabel,
            trendDescription = trendDescription,
            trendIsPositive = trendDirection > 0,
            trendIsNeutral = trendDirection == 0,
            consistencyPercent = consistencyPercent,
            consistencyDetail = consistencyDetail,
            projectedNetWorth = formatCurrency(projectedValue),
            projectedNetWorthDate = "by $projectedDate",
            currentStreakLabel = currentStreakLabel,
            currentStreakSubLabel = currentStreakSubLabel,
            monthlyEntries = buildMonthlyEntries(sorted, fullFormatter),
            hasData = true
        )
    }

    private fun applyFilter(data: List<MonthlyNetWorth>, filter: TimeFilter): List<MonthlyNetWorth> {
        val months = when (filter) {
            TimeFilter.THREE_MONTHS -> 3L
            TimeFilter.SIX_MONTHS -> 6L
            TimeFilter.TWELVE_MONTHS -> 12L
            TimeFilter.ALL -> return data
        }
        val cutoff = YearMonth.now().minusMonths(months)
        return data.filter { it.yearMonth >= cutoff }
    }

    private fun buildMonthlyEntries(
        sorted: List<MonthlyNetWorth>,
        formatter: DateTimeFormatter
    ): List<MonthlyEntryUiModel> {
        val reversed = sorted.reversed()
        return reversed.mapIndexed { i, month ->
            val prev = reversed.getOrNull(i + 1)
            if (prev == null) {
                MonthlyEntryUiModel(
                    monthLabel = month.yearMonth.format(formatter),
                    formattedValue = formatCurrency(month.value),
                    formattedDiff = "—",
                    formattedPercent = "",
                    isPositive = true,
                    isFirst = true
                )
            } else {
                val diff = month.value - prev.value
                val pct = if (prev.value != 0.0) (diff / prev.value) * 100.0 else 0.0
                MonthlyEntryUiModel(
                    monthLabel = month.yearMonth.format(formatter),
                    formattedValue = formatCurrency(month.value),
                    formattedDiff = formatChange(diff),
                    formattedPercent = formatPercent(pct),
                    isPositive = diff >= 0,
                    isFirst = false
                )
            }
        }
    }

    private fun formatCurrency(value: Double) =
        "$symbol${String.format(Locale.US, "%,.0f", value)}"

    private fun formatChange(value: Double): String {
        val absStr = String.format(Locale.US, "%,.0f", abs(value))
        return if (value >= 0) "+$symbol$absStr" else "-$symbol$absStr"
    }

    private fun formatPercent(pct: Double): String {
        val prefix = if (pct >= 0) "+" else ""
        return "$prefix${String.format(Locale.US, "%.1f", pct)}%"
    }
}
