package com.agcoding.networkapp.tax.presentation

import com.agcoding.networkapp.tax.domain.PARTNERSHIP_TAX_RATE_PERCENT

data class TaxResultUiModel(
    val formattedTurnover: String,
    val formattedExpenses: String,
    val formattedProfit: String,
    val formattedTax: String,
    val formattedProfitAfterTax: String,
    /** Tax ÷ 12: what to put aside every month to cover it */
    val formattedMonthlyReserve: String,
    val isLoss: Boolean,
)

data class TaxCalculatorUiState(
    val currencySymbol: String = "€",
    val ratePercent: Int = PARTNERSHIP_TAX_RATE_PERCENT.toInt(),
    val turnoverInput: String = "",
    val expensesInput: String = "",
    /** Null until a turnover is entered */
    val result: TaxResultUiModel? = null,
)

sealed interface TaxCalculatorIntent {
    data class UpdateTurnover(val value: String) : TaxCalculatorIntent
    data class UpdateExpenses(val value: String) : TaxCalculatorIntent
}
