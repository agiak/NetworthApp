package com.agcoding.networkapp.home.domain.usecase

import com.agcoding.networkapp.home.domain.model.NetWorthEntry
import java.time.YearMonth
import kotlin.math.abs

/**
 * Computes a "injection" map: for each month, how much of the total monthly change was due to
 * account additions (positive) or removals (negative) rather than organic portfolio performance.
 *
 * - Addition injection  > 0: a new account's starting balance showed up as "growth"
 * - Removal injection   < 0: a deleted account's balance showed up as "loss"
 *
 * Subtract this injection from the raw monthly diff to get the organic change.
 */
fun computeInjectionByMonth(rawEntries: List<NetWorthEntry>): Map<YearMonth, Double> {
    if (rawEntries.isEmpty()) return emptyMap()

    val realEntries = rawEntries.filter { it.note != NetWorthEntry.DELETION_MARKER }
    val tombstones  = rawEntries.filter { it.note == NetWorthEntry.DELETION_MARKER }

    val overallFirstMonth = realEntries.minOfOrNull { YearMonth.from(it.date) }
        ?: return emptyMap()

    val result = mutableMapOf<YearMonth, Double>()

    // ── Account ADDITIONS ──────────────────────────────────────────────────────
    // Any account whose first entry is after the portfolio's first-ever month is an "injection".
    realEntries.groupBy { it.accountId }.forEach { (_, entries) ->
        val firstMonth = entries.minOfOrNull { YearMonth.from(it.date) } ?: return@forEach
        if (firstMonth == overallFirstMonth) return@forEach
        val firstValue = entries
            .filter { YearMonth.from(it.date) == firstMonth }
            .maxByOrNull { it.date }?.value ?: return@forEach
        result[firstMonth] = (result[firstMonth] ?: 0.0) + firstValue
    }

    // ── Account REMOVALS ───────────────────────────────────────────────────────
    // A tombstone entry signals that the account was removed in that month.
    // The carried-forward balance (the last real value) vanishes as a "negative injection."
    tombstones.forEach { tombstone ->
        val removalMonth = YearMonth.from(tombstone.date)
        val lastValue = realEntries
            .filter { it.accountId == tombstone.accountId && YearMonth.from(it.date) <= removalMonth }
            .maxByOrNull { it.date }?.value ?: return@forEach
        result[removalMonth] = (result[removalMonth] ?: 0.0) - lastValue
    }

    return result
}

/** Formats an injection amount as a human-readable absolute value (no sign). */
fun formatInjectionAmount(injection: Double, currencySymbol: String): String {
    val absStr = String.format(java.util.Locale.US, "%,.0f", abs(injection))
    return "$currencySymbol$absStr"
}
