package com.agcoding.networkapp.fixedexpenses.domain.model

/** Cost of the expense spread over one month (annual expenses count as a twelfth). */
fun FixedExpense.monthlyCost(): Double = when (recurrence) {
    RecurrenceType.MONTHLY -> cost
    RecurrenceType.ANNUAL  -> cost / 12.0
}

/**
 * Accounts that carry this expense, each paying an equal share:
 * - no account assigned (= all) → every account
 * - assigned to several accounts (e.g. shared rent) → those accounts
 * - assigned only to accounts that no longer exist → every account
 */
fun FixedExpense.sharingAccountIds(allAccountIds: Collection<Long>): List<Long> {
    val assigned = accountIds.filter { it in allAccountIds }
    return assigned.ifEmpty { allAccountIds.toList() }
}

/**
 * Fraction of this expense (0.0–1.0) carried by [selectedAccountIds].
 * With no accounts at all the whole expense counts.
 */
fun FixedExpense.shareFor(selectedAccountIds: Collection<Long>, allAccountIds: Collection<Long>): Double {
    val sharing = sharingAccountIds(allAccountIds)
    if (sharing.isEmpty()) return 1.0
    return sharing.count { it in selectedAccountIds }.toDouble() / sharing.size
}
