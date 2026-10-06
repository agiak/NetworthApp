package com.agcoding.networkapp.savings.presentation.model

data class SavingsAccountUiModel(
    val accountId: Long,
    val accountName: String,
    val accountColorHex: String,
    val salaryRaw: Double,
    val hasSalary: Boolean,
    val expenseCount: Int,
    val formattedSalary: String,
    val formattedFixedExpenses: String,
    val formattedSavings: String,
    val formattedYearlySavings: String,
    /** 0–100, null when no salary is set */
    val savingsRatePercent: Int?,
    /** Savings as a fraction of salary (0f–1f), used for the progress bar */
    val savingsShare: Float,
    val isDeficit: Boolean,
)

data class SavingsSummaryUiModel(
    val formattedSalary: String,
    val formattedFixedExpenses: String,
    val formattedSavings: String,
    val formattedYearlySavings: String,
    val savingsRatePercent: Int?,
    val savingsShare: Float,
    val isDeficit: Boolean,
    val hasSalary: Boolean,
)
