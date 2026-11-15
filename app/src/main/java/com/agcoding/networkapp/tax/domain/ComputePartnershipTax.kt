package com.agcoding.networkapp.tax.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Income tax rate of a Greek partnership (Ε.Ε. / Ο.Ε.) on its profit. */
const val PARTNERSHIP_TAX_RATE_PERCENT = 22.0

data class PartnershipTax(
    val turnover: Double,
    val expenses: Double,
    /** Turnover − expenses; negative is a loss. */
    val profit: Double,
    /** Zero when there is no profit. */
    val tax: Double,
    val profitAfterTax: Double,
) {
    val isLoss: Boolean get() = profit < 0.0
}

/** Profit = [turnover] − [expenses]; the tax is [ratePercent] of the profit, rounded to the cent. */
fun computePartnershipTax(
    turnover: Double,
    expenses: Double,
    ratePercent: Double = PARTNERSHIP_TAX_RATE_PERCENT,
): PartnershipTax {
    val profit = cents(turnover - expenses)
    val tax = if (profit > 0.0) cents(profit * ratePercent / 100.0) else 0.0
    return PartnershipTax(
        turnover       = turnover,
        expenses       = expenses,
        profit         = profit,
        tax            = tax,
        profitAfterTax = cents(profit - tax),
    )
}

private fun cents(value: Double): Double = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
