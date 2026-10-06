package com.agcoding.networkapp.savings.presentation.calculator

data class SavingsProjectionUiModel(
    val years: Int,
    val formattedTotal: String,
    val formattedDeposits: String,
    /** Null when no annual return is set */
    val formattedReturns: String?,
    val isCustom: Boolean = false,
)

data class SavingsCalculatorUiState(
    val currencySymbol: String = "€",
    val monthlyAmountInput: String = "",
    val annualReturnInput: String = "",
    val customYearsInput: String = "",
    val projections: List<SavingsProjectionUiModel> = emptyList(),
    val customProjection: SavingsProjectionUiModel? = null,
)
