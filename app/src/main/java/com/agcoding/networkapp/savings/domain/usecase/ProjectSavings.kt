package com.agcoding.networkapp.savings.domain.usecase

import com.agcoding.networkapp.savings.domain.model.SavingsProjection
import kotlin.math.pow

/**
 * Amount collected after [years] of depositing [monthlyAmount] at the end of every month.
 * With an [annualReturnPercent] above zero the balance compounds monthly.
 */
fun projectSavings(
    monthlyAmount: Double,
    years: Int,
    annualReturnPercent: Double = 0.0,
): SavingsProjection {
    val months = years * 12
    val deposits = monthlyAmount * months
    val monthlyRate = annualReturnPercent / 100.0 / 12.0
    val total = if (monthlyRate <= 0.0) deposits
                else monthlyAmount * ((1 + monthlyRate).pow(months) - 1) / monthlyRate
    return SavingsProjection(years = years, total = total, deposits = deposits)
}
