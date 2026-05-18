package com.agcoding.networkapp.goal.presentation.mapper

import com.agcoding.networkapp.goal.presentation.GoalStatus
import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

class GoalUiMapper @Inject constructor() {

    private var symbol: String = "€"

    fun map(
        allData: List<MonthlyNetWorth>,
        targetAmount: Double,
        timeframeMonths: Int,
        currency: AppCurrency = AppCurrency.EUR,
    ): GoalMapResult {
        symbol = currency.symbol
        if (allData.isEmpty() || targetAmount <= 0 || timeframeMonths <= 0) return GoalMapResult()

        val sorted = allData.sortedBy { it.yearMonth }
        val currentNetWorth = sorted.last().value
        val diffs = sorted.zipWithNext().map { (a, b) -> b.value - a.value }
        val avgMonthlyGrowth = if (diffs.isNotEmpty()) diffs.average() else 0.0

        val targetFormatted = formatCurrency(targetAmount)
        val currentFormatted = formatCurrency(currentNetWorth)

        if (currentNetWorth >= targetAmount) {
            return GoalMapResult(
                hasData = true,
                currentNetWorthFormatted = currentFormatted,
                targetFormatted = targetFormatted,
                progressFraction = 1f,
                progressPercent = "100%",
                status = GoalStatus.GOAL_REACHED,
                requiredMonthly = formatCurrency(0.0),
                requiredYearly = formatCurrency(0.0),
                avgMonthlyGrowth = formatChange(avgMonthlyGrowth),
                fasterByPercent = 0,
                yearsAtCurrentPace = "—"
            )
        }

        val remaining = targetAmount - currentNetWorth
        val requiredMonthly = remaining / timeframeMonths
        val requiredYearly = requiredMonthly * 12

        val progressFraction = (currentNetWorth / targetAmount).toFloat().coerceIn(0f, 1f)
        val progressPercent = "${(progressFraction * 100).toInt()}%"

        val progressRatio = if (requiredMonthly > 0) avgMonthlyGrowth / requiredMonthly else 0.0
        val status = when {
            progressRatio >= 0.95 -> GoalStatus.ON_TRACK
            progressRatio >= 0.70 -> GoalStatus.SLIGHTLY_BEHIND
            else -> GoalStatus.FAR_FROM_GOAL
        }

        val fasterByPercent = when {
            progressRatio >= 1.0 -> 0
            progressRatio > 0 -> ((1.0 / progressRatio - 1.0) * 100).toInt().coerceAtMost(999)
            else -> 999
        }

        val yearsAtCurrentPace = if (avgMonthlyGrowth > 0) {
            val months = (remaining / avgMonthlyGrowth).toInt()
            when {
                months < 0 -> "—"
                months < 240 -> {
                    val yrs = months / 12
                    val mths = months % 12
                    if (mths == 0) "${yrs}Y" else "${yrs}Y ${mths}M"
                }
                else -> "20Y+"
            }
        } else "—"

        val (chartCurrent, chartRequired) = buildChartData(
            currentNetWorth, avgMonthlyGrowth, targetAmount, timeframeMonths
        )

        return GoalMapResult(
            hasData = true,
            currentNetWorthFormatted = currentFormatted,
            targetFormatted = targetFormatted,
            progressFraction = progressFraction,
            progressPercent = progressPercent,
            status = status,
            requiredMonthly = formatCurrency(requiredMonthly),
            requiredYearly = formatCurrency(requiredYearly),
            avgMonthlyGrowth = formatChange(avgMonthlyGrowth),
            fasterByPercent = fasterByPercent,
            yearsAtCurrentPace = yearsAtCurrentPace,
            chartCurrentTrajectory = chartCurrent,
            chartRequiredTrajectory = chartRequired,
            chartEndLabel = "${timeframeMonths / 12}Y"
        )
    }

    private fun buildChartData(
        currentValue: Double,
        avgMonthly: Double,
        targetAmount: Double,
        timeframeMonths: Int
    ): Pair<List<ChartPoint>, List<ChartPoint>> {
        val steps = 24
        val rawCurrent = (0..steps).map { i ->
            currentValue + avgMonthly * (i * timeframeMonths.toDouble() / steps)
        }
        val rawRequired = (0..steps).map { i ->
            currentValue + (targetAmount - currentValue) * (i.toDouble() / steps)
        }

        val allValues = rawCurrent + rawRequired
        val minVal = allValues.min()
        val maxVal = allValues.max()
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        fun toPoint(i: Int, v: Double) = ChartPoint(
            x = i.toFloat() / steps,
            y = ((v - minVal) / range).toFloat().coerceIn(0f, 1f)
        )

        return Pair(
            rawCurrent.mapIndexed { i, v -> toPoint(i, v) },
            rawRequired.mapIndexed { i, v -> toPoint(i, v) }
        )
    }

    private fun formatCurrency(value: Double): String {
        val prefix = if (value < 0) "-$symbol" else symbol
        return "$prefix${String.format(Locale.US, "%,.0f", abs(value))}"
    }

    private fun formatChange(value: Double): String {
        val absStr = String.format(Locale.US, "%,.0f", abs(value))
        return if (value >= 0) "+$symbol$absStr" else "-$symbol$absStr"
    }
}
