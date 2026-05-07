package com.agcoding.networkapp.analytics.presentation.prediction.mapper

import com.agcoding.networkapp.analytics.presentation.prediction.PredictionRange
import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

class PredictionUiMapper @Inject constructor() {

    fun map(allData: List<MonthlyNetWorth>, range: PredictionRange): PredictionMapResult {
        if (allData.size < 2) return PredictionMapResult()

        val sorted = allData.sortedBy { it.yearMonth }
        val lastValue = sorted.last().value
        val diffs = sorted.zipWithNext().map { (a, b) -> b.value - a.value }

        val avgGrowth = diffs.average()

        val variance = if (diffs.size > 1)
            diffs.map { (it - avgGrowth) * (it - avgGrowth) }.average()
        else (avgGrowth * 0.1) * (avgGrowth * 0.1)
        val rawStdDev = sqrt(variance)
        val σ = rawStdDev.coerceAtLeast(abs(avgGrowth) * 0.15 + 50.0)

        // Trend: compare the most recent 1/3 of history vs the older 2/3
        val splitIdx = (diffs.size * 2 / 3).coerceAtLeast(1).coerceAtMost(diffs.size - 1)
        val olderAvg = diffs.take(splitIdx).average()
        val recentAvg = diffs.drop(splitIdx).let { if (it.isEmpty()) avgGrowth else it.average() }
        val trendDelta = recentAvg - olderAvg // positive → improving, negative → declining

        // Conservative: lower monthly rate, with a small extra penalty if trend is declining
        val conservativeMonthly = if (avgGrowth > 0) {
            val trendPenalty = (-trendDelta).coerceAtLeast(0.0) * 0.15
            val raw = avgGrowth - σ * 0.7 - trendPenalty
            raw.coerceAtLeast(avgGrowth * 0.15)   // floor at 15 % of avg, never 0
        } else {
            avgGrowth - σ * 0.3
        }

        // Optimistic: moderate boost, with a small uplift if trend is already positive
        val optimisticMonthly = if (avgGrowth > 0) {
            val trendBoost = trendDelta.coerceAtLeast(0.0) * 0.15
            avgGrowth + σ * 0.7 + trendBoost
        } else {
            avgGrowth + σ * 0.4  // declining avg → assume partial recovery
        }

        val months = range.months.toDouble()
        val expected = lastValue + avgGrowth * months
        val minimum = lastValue + conservativeMonthly * months   // intentionally NOT clamped to 0
        val maximum = lastValue + optimisticMonthly * months

        val (chartExpected, chartMinimum, chartMaximum) =
            buildChartSeries(lastValue, avgGrowth, conservativeMonthly, optimisticMonthly, range)

        return PredictionMapResult(
            hasData = true,
            expectedValue = formatCurrency(expected),
            minimumValue = formatCurrency(minimum),
            maximumValue = formatCurrency(maximum),
            avgMonthlyGrowth = formatChange(avgGrowth),
            conservativeMonthlyRate = formatChange(conservativeMonthly),
            optimisticMonthlyRate = formatChange(optimisticMonthly),
            chartExpected = chartExpected,
            chartMinimum = chartMinimum,
            chartMaximum = chartMaximum,
            chartExpectedLabel = formatCompact(expected),
            chartMinimumLabel = formatCompact(minimum),
            chartMaximumLabel = formatCompact(maximum),
            chartMidLabel = midLabel(range),
            chartEndLabel = "${range.years}Y"
        )
    }

    private fun buildChartSeries(
        startValue: Double,
        avgMonthly: Double,
        conservativeMonthly: Double,
        optimisticMonthly: Double,
        range: PredictionRange
    ): Triple<List<ChartPoint>, List<ChartPoint>, List<ChartPoint>> {
        val steps = 24
        val totalMonths = range.months.toDouble()

        val rawExpected = (0..steps).map { i -> startValue + avgMonthly * (i * totalMonths / steps) }
        val rawMin = (0..steps).map { i -> startValue + conservativeMonthly * (i * totalMonths / steps) }
        val rawMax = (0..steps).map { i -> startValue + optimisticMonthly * (i * totalMonths / steps) }

        val allValues = rawExpected + rawMin + rawMax
        val minVal = allValues.min()
        val maxVal = allValues.max()
        val valueRange = (maxVal - minVal).coerceAtLeast(1.0)

        fun toPoint(i: Int, v: Double) = ChartPoint(
            x = i.toFloat() / steps,
            y = ((v - minVal) / valueRange).toFloat().coerceIn(0f, 1f)
        )

        return Triple(
            rawExpected.mapIndexed { i, v -> toPoint(i, v) },
            rawMin.mapIndexed { i, v -> toPoint(i, v) },
            rawMax.mapIndexed { i, v -> toPoint(i, v) }
        )
    }

    private fun midLabel(range: PredictionRange): String = when (range) {
        PredictionRange.TWO_YEARS -> "1Y"
        PredictionRange.THREE_YEARS -> "1.5Y"
        PredictionRange.FIVE_YEARS -> "2.5Y"
        PredictionRange.TEN_YEARS -> "5Y"
        PredictionRange.FIFTEEN_YEARS -> "7.5Y"
    }

    private fun formatCurrency(value: Double): String {
        val prefix = if (value < 0) "-€" else "€"
        return "$prefix${String.format(Locale.US, "%,.0f", abs(value))}"
    }

    private fun formatCompact(value: Double): String {
        val absVal = abs(value)
        val prefix = if (value < 0) "-€" else "€"
        return when {
            absVal >= 1_000_000 -> "${prefix}${String.format(Locale.US, "%.1f", absVal / 1_000_000)}M"
            absVal >= 1_000 -> "${prefix}${String.format(Locale.US, "%.0f", absVal / 1_000)}K"
            else -> formatCurrency(value)
        }
    }

    private fun formatChange(value: Double): String {
        val absStr = String.format(Locale.US, "%,.0f", abs(value))
        return if (value >= 0) "+€$absStr" else "-€$absStr"
    }
}
