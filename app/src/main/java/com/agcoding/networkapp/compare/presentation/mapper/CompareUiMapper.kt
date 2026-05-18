package com.agcoding.networkapp.compare.presentation.mapper

import com.agcoding.networkapp.compare.presentation.CompareMode
import com.agcoding.networkapp.compare.presentation.CompareSpeed
import com.agcoding.networkapp.compare.presentation.CompareStability
import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

class CompareUiMapper @Inject constructor() {

    private val periodFormatter = DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault())
    private val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())

    companion object {
        private const val MS_PER_DAY = 86_400_000L
    }

    fun map(
        allData: List<MonthlyNetWorth>,
        mode: CompareMode,
        customCurrentStart: Long? = null,
        customCurrentEnd: Long? = null,
        customPreviousStart: Long? = null,
        customPreviousEnd: Long? = null
    ): CompareMapResult {
        if (allData.isEmpty()) return CompareMapResult()

        val sorted = allData.sortedBy { it.yearMonth }
        val now = YearMonth.now()

        val (currentEntries, previousEntries) = when (mode) {
            CompareMode.YEAR_VS_YEAR -> {
                val curYear = now.year
                Pair(
                    sorted.filter { it.yearMonth.year == curYear },
                    sorted.filter { it.yearMonth.year == curYear - 1 }
                )
            }
            CompareMode.HALF_VS_HALF -> {
                val months = sorted.map { it.yearMonth }.distinct().sortedDescending()
                Pair(
                    sorted.filter { it.yearMonth in months.take(6).toSet() },
                    sorted.filter { it.yearMonth in months.drop(6).take(6).toSet() }
                )
            }
            CompareMode.QUARTER_VS_QUARTER -> {
                val months = sorted.map { it.yearMonth }.distinct().sortedDescending()
                Pair(
                    sorted.filter { it.yearMonth in months.take(3).toSet() },
                    sorted.filter { it.yearMonth in months.drop(3).take(3).toSet() }
                )
            }
            CompareMode.YEAR_ROLLING -> {
                val months = sorted.map { it.yearMonth }.distinct().sortedDescending()
                Pair(
                    sorted.filter { it.yearMonth in months.take(12).toSet() },
                    sorted.filter { it.yearMonth in months.drop(12).take(12).toSet() }
                )
            }
            CompareMode.CUSTOM -> {
                val curStart = customCurrentStart?.let { YearMonth.from(LocalDate.ofEpochDay(it / MS_PER_DAY)) }
                val curEnd = customCurrentEnd?.let { YearMonth.from(LocalDate.ofEpochDay(it / MS_PER_DAY)) }
                val prevStart = customPreviousStart?.let { YearMonth.from(LocalDate.ofEpochDay(it / MS_PER_DAY)) }
                val prevEnd = customPreviousEnd?.let { YearMonth.from(LocalDate.ofEpochDay(it / MS_PER_DAY)) }
                if (curStart == null || curEnd == null || prevStart == null || prevEnd == null) {
                    return CompareMapResult()
                }
                Pair(
                    sorted.filter { it.yearMonth >= curStart && it.yearMonth <= curEnd },
                    sorted.filter { it.yearMonth >= prevStart && it.yearMonth <= prevEnd }
                )
            }
        }

        if (currentEntries.size < 2 || previousEntries.size < 2) return CompareMapResult()

        val currentStats = computePeriodStats(currentEntries) ?: return CompareMapResult()
        val previousStats = computePeriodStats(previousEntries) ?: return CompareMapResult()

        val currentLabel = buildPeriodLabel(mode, currentEntries, isCurrent = true)
        val previousLabel = buildPeriodLabel(mode, previousEntries, isCurrent = false)

        val growthDiffRaw = currentStats.totalGrowth - previousStats.totalGrowth
        val avgDiffRaw = currentStats.avgMonthly - previousStats.avgMonthly
        val avgChangePct = if (previousStats.avgMonthly != 0.0) {
            (avgDiffRaw / abs(previousStats.avgMonthly)) * 100.0
        } else 0.0

        val speedRatio = when {
            previousStats.avgMonthly <= 0.0 && currentStats.avgMonthly > 0.0 -> 2.0
            previousStats.avgMonthly == 0.0 -> 1.0
            else -> currentStats.avgMonthly / previousStats.avgMonthly
        }
        val speed = when {
            speedRatio > 1.15 -> CompareSpeed.ACCELERATING
            speedRatio < 0.85 -> CompareSpeed.SLOWING
            else -> CompareSpeed.STABLE
        }

        val volDiff = currentStats.volatility - previousStats.volatility
        val volThreshold = previousStats.volatility * 0.1
        val stability = when {
            previousStats.volatility > 0 && volDiff < -volThreshold -> CompareStability.MORE_STABLE
            previousStats.volatility > 0 && volDiff > volThreshold -> CompareStability.LESS_STABLE
            else -> CompareStability.SIMILAR
        }

        return CompareMapResult(
            hasData = true,
            currentLabel = currentLabel,
            previousLabel = previousLabel,
            currentTotalGrowth = formatChange(currentStats.totalGrowth),
            previousTotalGrowth = formatChange(previousStats.totalGrowth),
            currentTotalGrowthPositive = currentStats.totalGrowth >= 0,
            previousTotalGrowthPositive = previousStats.totalGrowth >= 0,
            growthDiff = formatCurrency(abs(growthDiffRaw)),
            growthImproved = growthDiffRaw >= 0,
            currentAvgMonthly = formatChange(currentStats.avgMonthly),
            previousAvgMonthly = formatChange(previousStats.avgMonthly),
            avgMonthlyDiff = formatCurrency(abs(avgDiffRaw)),
            avgMonthlyImproved = avgDiffRaw >= 0,
            avgMonthlyChangePercent = formatPercent(avgChangePct),
            speed = speed,
            stability = stability,
            stabilityCurrentVolatility = formatCurrency(currentStats.volatility),
            stabilityPreviousVolatility = formatCurrency(previousStats.volatility),
            bestMonthLabel = currentStats.bestMonthEntry?.first?.yearMonth?.format(monthFormatter) ?: "",
            bestMonthValue = currentStats.bestMonthEntry?.second?.let { formatChange(it) } ?: "",
            currentChartData = currentStats.chartData,
            previousChartData = previousStats.chartData,
        )
    }

    private fun buildPeriodLabel(
        mode: CompareMode,
        entries: List<MonthlyNetWorth>,
        isCurrent: Boolean
    ): String = when (mode) {
        CompareMode.YEAR_VS_YEAR -> entries.firstOrNull()?.yearMonth?.year?.toString() ?: ""
        CompareMode.QUARTER_VS_QUARTER -> if (isCurrent)
            stringForRange(entries.minOf { it.yearMonth }, entries.maxOf { it.yearMonth })
        else
            stringForRange(entries.minOf { it.yearMonth }, entries.maxOf { it.yearMonth })
        else -> formatRangePeriod(entries)
    }

    private fun stringForRange(start: YearMonth, end: YearMonth): String {
        return if (start == end) start.format(periodFormatter)
        else "${start.format(periodFormatter)} – ${end.format(periodFormatter)}"
    }

    private fun formatRangePeriod(entries: List<MonthlyNetWorth>): String {
        val s = entries.sortedBy { it.yearMonth }
        val start = s.firstOrNull()?.yearMonth ?: return ""
        val end = s.lastOrNull()?.yearMonth ?: return ""
        return stringForRange(start, end)
    }

    private data class PeriodStats(
        val totalGrowth: Double,
        val avgMonthly: Double,
        val volatility: Double,
        val chartData: List<ChartPoint>,
        val bestMonthEntry: Pair<MonthlyNetWorth, Double>?
    )

    private fun computePeriodStats(entries: List<MonthlyNetWorth>): PeriodStats? {
        if (entries.size < 2) return null
        val sorted = entries.sortedBy { it.yearMonth }
        val values = sorted.map { it.value }
        val diffs = values.zipWithNext { a, b -> b - a }

        val totalGrowth = values.last() - values.first()
        val avgMonthly = diffs.average()
        val volatility = stdDev(diffs)

        val minVal = values.min()
        val maxVal = values.max()
        val range = (maxVal - minVal).coerceAtLeast(1.0)
        val n = sorted.size - 1
        val chartData = sorted.mapIndexed { i, m ->
            ChartPoint(
                x = i.toFloat() / n,
                y = ((m.value - minVal) / range).toFloat().coerceIn(0f, 1f)
            )
        }

        val diffPairs = sorted.zipWithNext { a, b -> Pair(b, b.value - a.value) }
        val bestEntry = diffPairs.maxByOrNull { it.second }

        return PeriodStats(
            totalGrowth = totalGrowth,
            avgMonthly = avgMonthly,
            volatility = volatility,
            chartData = chartData,
            bestMonthEntry = bestEntry
        )
    }

    private fun stdDev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return sqrt(variance)
    }

    private fun formatCurrency(value: Double): String {
        val prefix = if (value < 0) "-€" else "€"
        return "$prefix${String.format(Locale.US, "%,.0f", abs(value))}"
    }

    private fun formatChange(value: Double): String {
        val absStr = String.format(Locale.US, "%,.0f", abs(value))
        return if (value >= 0) "+€$absStr" else "-€$absStr"
    }

    private fun formatPercent(pct: Double): String {
        val prefix = if (pct >= 0) "+" else ""
        return "$prefix${String.format(Locale.US, "%.0f", pct)}%"
    }
}
