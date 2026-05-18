package com.agcoding.networkapp.home.presentation.mapper

import com.agcoding.networkapp.home.domain.model.MonthlyNetWorth
import com.agcoding.networkapp.home.presentation.model.ChartPoint
import com.agcoding.networkapp.home.presentation.model.InsightData
import com.agcoding.networkapp.home.presentation.model.NetWorthDisplayData
import com.agcoding.networkapp.settings.domain.model.AppCurrency
import com.agcoding.networkapp.shared.utils.formatForDisplay
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil

class NetWorthDomainToUiMapper @Inject constructor() {

    fun map(monthlyData: List<MonthlyNetWorth>, currency: AppCurrency = AppCurrency.EUR): NetWorthDisplayData {
        if (monthlyData.isEmpty()) return NetWorthDisplayData()

        val sortedDesc = monthlyData.sortedByDescending { it.yearMonth }
        val latest = sortedDesc.first()
        val previous = sortedDesc.drop(1).firstOrNull()

        val changeThisMonth: String
        val changePercentage: String
        val isPositiveChange: Boolean
        if (previous != null) {
            val diff = latest.value - previous.value
            val pct = if (previous.value != 0.0) (diff / previous.value) * 100.0 else 0.0
            isPositiveChange = diff >= 0
            changeThisMonth = diff.formatAsCurrency(currency)
            changePercentage = "${if (pct >= 0) "+ " else "- "}${String.format(Locale.US, "%.1f", Math.abs(pct))} %"
        } else {
            changeThisMonth = "${currency.symbol}0"
            changePercentage = "0 %"
            isPositiveChange = true
        }

        val chartEntries = sortedDesc.take(12).reversed()
        val minVal = chartEntries.minOfOrNull { it.value } ?: 0.0
        val maxVal = chartEntries.maxOfOrNull { it.value } ?: 0.0
        val range = if (maxVal == minVal) 1.0 else maxVal - minVal
        val chartData = chartEntries.mapIndexed { i, m ->
            ChartPoint(
                x = if (chartEntries.size > 1) i.toFloat() / (chartEntries.size - 1) else 0.5f,
                y = ((m.value - minVal) / range).toFloat()
            )
        }

        // Fix YTD Growth: compare against the last month of the previous year if available
        val currentYear = latest.yearMonth.year
        val lastYearEntry = sortedDesc.firstOrNull { it.yearMonth.year < currentYear }
        val ytdStartValue = lastYearEntry?.value ?: sortedDesc.last().value
        
        val ytdDiff = latest.value - ytdStartValue
        val ytdPct = if (ytdStartValue != 0.0) (ytdDiff / ytdStartValue) * 100 else 0.0

        val ytdGrowth = ytdDiff.formatAsChange(currency)
        val ytdPercentage = "${if (ytdPct >= 0) "+" else ""}${String.format(Locale.US, "%.0f", Math.abs(ytdPct))}%"

        val avgPerMonth: String = if (sortedDesc.size >= 2) {
            val avg = (sortedDesc.first().value - sortedDesc.last().value) / (sortedDesc.size - 1)
            avg.formatAsCurrency(currency)
        } else "${currency.symbol}0"

        val streak = buildStreakCount(sortedDesc)

        return NetWorthDisplayData(
            currentNetWorth = latest.value.formatAsCurrency(currency),
            changeThisMonth = changeThisMonth,
            changePercentage = changePercentage,
            isPositiveChange = isPositiveChange,
            lastUpdatedDate = latest.lastUpdatedDate.formatForDisplay(),
            chartData = chartData,
            insights = buildInsights(sortedDesc),
            ytdGrowth = ytdGrowth,
            ytdPercentage = ytdPercentage,
            avgPerMonth = avgPerMonth,
            streakMonths = streak,
            isStreakPositive = streak > 0
        )
    }

    private fun buildStreakCount(sortedDesc: List<MonthlyNetWorth>): Int {
        var streak = 0
        for (i in 0 until sortedDesc.size - 1) {
            if (sortedDesc[i].value > sortedDesc[i + 1].value) streak++ else break
        }
        return streak
    }

    private fun buildInsights(sortedDesc: List<MonthlyNetWorth>): List<InsightData> {
        if (sortedDesc.size < 2) return emptyList()
        val insights = mutableListOf<InsightData>()

        val streak = buildStreakCount(sortedDesc)
        if (streak > 0) insights.add(InsightData.GrowthStreak(streakMonths = streak))

        val monthlyIncreases = (0 until sortedDesc.size - 1).map { i ->
            sortedDesc[i].yearMonth to (sortedDesc[i].value - sortedDesc[i + 1].value)
        }
        val best = monthlyIncreases.maxByOrNull { it.second }
        if (best != null && best.second > 0) {
            insights.add(InsightData.BestMonth(yearMonth = best.first, growthAmount = best.second))
        }

        if (sortedDesc.size >= 3) {
            val current = sortedDesc.first().value
            val avgGrowth = (current - sortedDesc.last().value) / (sortedDesc.size - 1)
            if (avgGrowth > 0) {
                val step = when {
                    current < 10_000.0 -> 5_000.0
                    current < 50_000.0 -> 10_000.0
                    current < 100_000.0 -> 25_000.0
                    else -> 50_000.0
                }
                val nextMilestone = ceil(current / step) * step
                val months = ceil((nextMilestone - current) / avgGrowth).toInt().coerceAtLeast(1)
                insights.add(InsightData.Forecast(targetAmount = nextMilestone, targetYearMonth = YearMonth.now().plusMonths(months.toLong())))
            }
        }

        return insights.take(3)
    }

    private fun Double.formatAsCurrency(currency: AppCurrency): String =
        "${currency.symbol}${String.format(Locale.US, "%,.0f", this)}"

    private fun Double.formatAsChange(currency: AppCurrency): String {
        val prefix = if (this >= 0) "+" else ""
        return "$prefix${currency.symbol}${String.format(Locale.US, "%,.0f", this)}"
    }
}
