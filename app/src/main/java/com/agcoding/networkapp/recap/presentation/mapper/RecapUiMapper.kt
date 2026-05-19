package com.agcoding.networkapp.recap.presentation.mapper

import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.recap.presentation.MonthlyBreakdownItem
import com.agcoding.networkapp.recap.presentation.RecapTrend
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

class RecapUiMapper @Inject constructor() {

    private var symbol: String = "€"

    private val shortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
    private val fullMonthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())

    fun map(
        allData: List<MonthlyNetWorth>,
        year: Int,
        targetAmount: Double,
        currency: AppCurrency = AppCurrency.EUR,
    ): RecapMapResult {
        symbol = currency.symbol
        if (allData.isEmpty()) return RecapMapResult()

        val yearData = allData.filter { it.yearMonth.year == year }.sortedBy { it.yearMonth }
        if (yearData.isEmpty()) return RecapMapResult()

        val sorted = allData.sortedBy { it.yearMonth }

        // Start value: last entry before this year, OR first entry of this year
        val prevEntry = sorted.lastOrNull { it.yearMonth.year < year }
        val startValue = prevEntry?.value ?: yearData.first().value
        val endValue = yearData.last().value

        val totalGrowth = endValue - startValue
        val totalGrowthPct = if (startValue > 0) (totalGrowth / startValue) * 100.0 else 0.0

        // Monthly diffs: prev year's last → through all year entries
        val dataForDiffs = if (prevEntry != null) listOf(prevEntry) + yearData else yearData
        val diffs = dataForDiffs.zipWithNext().map { (a, b) -> b to (b.value - a.value) }

        val bestEntry = diffs.maxByOrNull { it.second }
        val worstEntry = diffs.minByOrNull { it.second }
        val avgGrowth = if (diffs.isNotEmpty()) diffs.map { it.second }.average() else 0.0

        // Trend: compare first vs second half of year diffs
        val trend = if (diffs.size >= 4) {
            val mid = diffs.size / 2
            val firstHalfAvg = diffs.take(mid).map { it.second }.average()
            val secondHalfAvg = diffs.drop(mid).map { it.second }.average()
            when {
                totalGrowth < 0 -> RecapTrend.DECLINED
                secondHalfAvg > firstHalfAvg * 1.1 -> RecapTrend.ACCELERATED
                firstHalfAvg > secondHalfAvg * 1.1 -> RecapTrend.DECELERATED
                else -> RecapTrend.STABLE
            }
        } else {
            if (totalGrowth >= 0) RecapTrend.STABLE else RecapTrend.DECLINED
        }

        // Chart: year months only, normalized
        val chartData = buildChartData(yearData)

        // Monthly breakdown
        val monthlyBreakdown = buildMonthlyBreakdown(dataForDiffs)

        // Goal progress
        val goalProgress = if (targetAmount > 0) (endValue / targetAmount).toFloat().coerceIn(0f, 1f) else 0f

        // All-time high: did this year set a new record?
        val allTimeHighValue = allData.maxOf { it.value }
        val isNewAllTimeHigh = yearData.any { it.value >= allTimeHighValue }

        return RecapMapResult(
            hasData = true,
            startValue = formatCurrency(startValue),
            endValue = formatCurrency(endValue),
            totalGrowthFormatted = formatChange(totalGrowth),
            totalGrowthPercent = formatPercent(totalGrowthPct),
            totalGrowthPositive = totalGrowth >= 0,
            bestMonthLabel = bestEntry?.first?.yearMonth?.format(shortMonthFormatter) ?: "",
            bestMonthValue = bestEntry?.second?.let { formatChange(it) } ?: "",
            worstMonthLabel = worstEntry?.first?.yearMonth?.format(shortMonthFormatter) ?: "",
            worstMonthValue = worstEntry?.second?.let { formatChange(it) } ?: "",
            avgMonthlyGrowth = formatChange(avgGrowth),
            biggestJump = bestEntry?.second?.let { formatChange(it) } ?: "",
            hasGoal = targetAmount > 0,
            goalProgress = goalProgress,
            goalProgressPercent = if (targetAmount > 0) "${(goalProgress * 100).toInt()}%" else "",
            goalYearContribution = formatChange(totalGrowth),
            trend = trend,
            isNewAllTimeHigh = isNewAllTimeHigh,
            monthsTracked = yearData.size,
            chartData = chartData,
            chartStartLabel = yearData.first().yearMonth.format(shortMonthFormatter),
            chartEndLabel = yearData.last().yearMonth.format(shortMonthFormatter),
            monthlyBreakdown = monthlyBreakdown
        )
    }

    private fun buildChartData(yearData: List<MonthlyNetWorth>): List<ChartPoint> {
        if (yearData.size < 2) return emptyList()
        val minVal = yearData.minOf { it.value }
        val maxVal = yearData.maxOf { it.value }
        val range = (maxVal - minVal).coerceAtLeast(1.0)
        val n = yearData.size - 1
        return yearData.mapIndexed { i, m ->
            ChartPoint(
                x = if (n > 0) i.toFloat() / n else 0.5f,
                y = ((m.value - minVal) / range).toFloat().coerceIn(0f, 1f)
            )
        }
    }

    private fun buildMonthlyBreakdown(
        dataForDiffs: List<MonthlyNetWorth>
    ): List<MonthlyBreakdownItem> {
        return dataForDiffs.zipWithNext().mapIndexed { _, (prev, curr) ->
            val diff = curr.value - prev.value
            val pct = if (prev.value != 0.0) (diff / prev.value) * 100.0 else 0.0
            MonthlyBreakdownItem(
                monthLabel = curr.yearMonth.format(fullMonthFormatter),
                formattedValue = formatCurrency(curr.value),
                formattedChange = formatChange(diff),
                isPositive = diff >= 0,
                isFirst = false
            )
        }
    }

    private fun formatCurrency(value: Double): String {
        val prefix = if (value < 0) "-$symbol" else symbol
        return "$prefix${String.format(Locale.US, "%,.0f", abs(value))}"
    }

    private fun formatChange(value: Double): String {
        val absStr = String.format(Locale.US, "%,.0f", abs(value))
        return if (value >= 0) "+$symbol$absStr" else "-$symbol$absStr"
    }

    private fun formatPercent(pct: Double): String {
        val prefix = if (pct >= 0) "+" else ""
        return "$prefix${String.format(Locale.US, "%.1f", pct)}%"
    }
}
