package com.agcoding.networkapp.analytics.presentation.prediction.mapper

import com.agcoding.networkapp.analytics.presentation.prediction.PredictionRange
import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

class PredictionUiMapper @Inject constructor() {

    private var symbol: String = "€"

    fun map(allData: List<MonthlyNetWorth>, range: PredictionRange, currency: AppCurrency = AppCurrency.EUR): PredictionMapResult {
        symbol = currency.symbol
        if (allData.size < 2) return PredictionMapResult()

        val sorted = allData.sortedBy { it.yearMonth }
        val lastValue = sorted.last().value
        val diffs = sorted.zipWithNext().map { (a, b) -> b.value - a.value }
        val n = diffs.size

        // ── Baseline stats ────────────────────────────────────────────────────
        val avgGrowth = diffs.average()

        val variance = if (n > 1)
            diffs.map { (it - avgGrowth) * (it - avgGrowth) }.average()
        else (avgGrowth * 0.1) * (avgGrowth * 0.1)
        // Minimum uncertainty floor so bands are always visible
        val σ = sqrt(variance).coerceAtLeast(abs(avgGrowth) * 0.15 + 50.0)

        // ── Weighted average (linear weights, recent = higher weight) ─────────
        // Month 1 (oldest) gets weight 1, month n (newest) gets weight n
        val weightSum = n * (n + 1) / 2.0
        val weightedAvg = diffs.mapIndexed { i, d -> d * (i + 1) }.sum() / weightSum

        // ── Acceleration analysis (half-split) ───────────────────────────────
        val halveMid = (n / 2).coerceAtLeast(1)
        val firstHalfAvg = diffs.take(halveMid).average()
        val secondHalfAvg = diffs.drop(halveMid).let { if (it.isEmpty()) avgGrowth else it.average() }
        val acceleration = secondHalfAvg - firstHalfAvg

        // Acceleration is meaningful only with 4+ data points
        val hasAccelerationData = n >= 4
        val isAccelerating = hasAccelerationData && acceleration > 0

        val months = range.months.toDouble()

        // ── Expected: weighted average captures recent momentum ───────────────
        val expectedMonthly = if (hasAccelerationData) weightedAvg else avgGrowth
        val expected = lastValue + expectedMonthly * months

        // ── Conservative: models deceleration or stall ────────────────────────
        val conservativeMonthly = computeConservativeRate(
            isAccelerating, hasAccelerationData, avgGrowth,
            firstHalfAvg, weightedAvg, σ, acceleration
        )
        val minimum = lastValue + conservativeMonthly * months

        // ── Maximum (Momentum): projects acceleration forward, dampened ────────
        val (optimisticAvgRate, optimisticEndRate) = computeMomentumRates(
            isAccelerating, hasAccelerationData,
            secondHalfAvg, weightedAvg, acceleration, σ, months
        )
        val maximum = lastValue + optimisticAvgRate * months

        // ── Chart data ────────────────────────────────────────────────────────
        val (chartExpected, chartMinimum, chartMaximum) = buildChartSeries(
            startValue = lastValue,
            expectedMonthly = expectedMonthly,
            conservativeMonthly = conservativeMonthly,
            optimisticStartRate = if (hasAccelerationData) secondHalfAvg else avgGrowth,
            optimisticEndRate = optimisticEndRate,
            range = range
        )

        return PredictionMapResult(
            hasData = true,
            expectedValue = formatCurrency(expected),
            minimumValue = formatCurrency(minimum),
            maximumValue = formatCurrency(maximum),
            avgMonthlyGrowth = formatChange(avgGrowth),
            conservativeMonthlyRate = formatChange(conservativeMonthly),
            optimisticMonthlyRate = formatChange(optimisticEndRate),
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

    /**
     * Conservative: models what happens if momentum stalls or decelerates.
     *
     * - If growth was accelerating   → assume it stalls back toward the older average
     * - If growth was flat/declining → assume the deceleration continues modestly
     */
    private fun computeConservativeRate(
        isAccelerating: Boolean,
        hasAccelerationData: Boolean,
        avgGrowth: Double,
        firstHalfAvg: Double,
        weightedAvg: Double,
        σ: Double,
        acceleration: Double
    ): Double {
        val floor = if (avgGrowth > 0) avgGrowth * 0.15 else avgGrowth * 1.25
        return when {
            !hasAccelerationData -> {
                // Not enough history: σ-based conservative
                if (avgGrowth > 0) (avgGrowth - σ * 0.7).coerceAtLeast(floor)
                else avgGrowth - σ * 0.3
            }
            isAccelerating -> {
                // Was accelerating → conservative: growth stalls, reverts toward older era
                val stalledRate = firstHalfAvg - σ * 0.2
                stalledRate.coerceAtLeast(floor)
            }
            else -> {
                // Was flat or decelerating → conservative: trend continues (acceleration is negative)
                val trendPenalty = (-acceleration).coerceAtLeast(0.0) * 0.2
                val decelerated = weightedAvg - σ * 0.65 - trendPenalty
                if (avgGrowth > 0) decelerated.coerceAtLeast(floor) else decelerated
            }
        }
    }

    /**
     * Momentum (maximum): projects acceleration forward using a dampened S-curve.
     *
     * - Start rate = current secondHalfAvg (where the user is right now)
     * - End rate   = startRate + acceleration × dampingFactor(months)
     * - Average rate (trapezoidal) = (startRate + endRate) / 2
     *
     * The S-curve f(m) = 1 − e^(−m/48) means:
     *   12m  → 22%,  24m → 39%,  60m → 71%,  120m → 92%  of full acceleration captured.
     *
     * Returns Pair(avgRateForFinalValue, endRateForLabel)
     */
    private fun computeMomentumRates(
        isAccelerating: Boolean,
        hasAccelerationData: Boolean,
        secondHalfAvg: Double,
        weightedAvg: Double,
        acceleration: Double,
        σ: Double,
        months: Double
    ): Pair<Double, Double> {
        if (!hasAccelerationData || !isAccelerating) {
            // Fallback: flat optimistic rate (weighted avg + modest σ boost)
            val rate = (if (isAccelerating) secondHalfAvg else weightedAvg) + σ * 0.5
            return Pair(rate, rate)
        }

        val dampingFactor = 1.0 - exp(-months / 48.0)
        val rawEndRate = secondHalfAvg + acceleration * dampingFactor

        // Cap: never more than 2× the acceleration magnitude above the start rate
        val maxBoost = minOf(
            abs(acceleration) * 2.0,
            abs(secondHalfAvg).coerceAtLeast(σ) * 1.5
        )
        val endRate = rawEndRate
            .coerceAtLeast(secondHalfAvg)          // never reduce the rate in the optimistic scenario
            .coerceAtMost(secondHalfAvg + maxBoost) // cap unrealistic exponential projections

        // Trapezoidal average: rate ramps linearly from start to end over the projection period
        val avgRate = (secondHalfAvg + endRate) / 2.0

        return Pair(avgRate, endRate)
    }

    /**
     * Chart series:
     * - Expected & Conservative: straight lines (constant monthly rate)
     * - Maximum (Momentum): curved line — rate increases linearly from startRate to endRate
     */
    private fun buildChartSeries(
        startValue: Double,
        expectedMonthly: Double,
        conservativeMonthly: Double,
        optimisticStartRate: Double,
        optimisticEndRate: Double,
        range: PredictionRange
    ): Triple<List<ChartPoint>, List<ChartPoint>, List<ChartPoint>> {
        val steps = 24
        val totalMonths = range.months.toDouble()

        val rawExpected = (0..steps).map { i ->
            startValue + expectedMonthly * (i * totalMonths / steps)
        }
        val rawMin = (0..steps).map { i ->
            startValue + conservativeMonthly * (i * totalMonths / steps)
        }

        // Maximum: curved projection — rate ramps from startRate → endRate
        val rawMax = buildList {
            add(startValue)
            var value = startValue
            val rateRange = optimisticEndRate - optimisticStartRate
            for (s in 1..steps) {
                val stepMonths = totalMonths / steps
                val progress = if (steps > 1) (s - 1).toDouble() / (steps - 1) else 1.0
                val rateAtStep = optimisticStartRate + rateRange * progress
                value += rateAtStep * stepMonths
                add(value)
            }
        }

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
        val prefix = if (value < 0) "-$symbol" else symbol
        return "$prefix${String.format(Locale.US, "%,.0f", abs(value))}"
    }

    private fun formatCompact(value: Double): String {
        val absVal = abs(value)
        val prefix = if (value < 0) "-$symbol" else symbol
        return when {
            absVal >= 1_000_000 -> "${prefix}${String.format(Locale.US, "%.1f", absVal / 1_000_000)}M"
            absVal >= 1_000 -> "${prefix}${String.format(Locale.US, "%.0f", absVal / 1_000)}K"
            else -> formatCurrency(value)
        }
    }

    private fun formatChange(value: Double): String {
        val absStr = String.format(Locale.US, "%,.0f", abs(value))
        return if (value >= 0) "+$symbol$absStr" else "-$symbol$absStr"
    }
}
