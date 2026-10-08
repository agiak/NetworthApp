package com.agcoding.networkapp.expenseanalysis.domain.model

enum class CategorySource {
    /** The user picked the category for this exact name. */
    MANUAL,
    /** Words the user has categorised before decided it. */
    LEARNED,
    /** Built-in keyword dictionary decided it. */
    AUTO,
    /** Nothing matched: falls back to [ExpenseCategory.OTHER]. */
    NONE,
}

data class CategoryPrediction(
    val category: ExpenseCategory,
    /** 0.0–1.0, how sure the categoriser is. */
    val confidence: Double,
    val source: CategorySource,
) {
    /** Worth asking the user to confirm. */
    val isUncertain: Boolean
        get() = source == CategorySource.NONE ||
                (source != CategorySource.MANUAL && confidence < UNCERTAIN_BELOW)

    companion object {
        const val UNCERTAIN_BELOW = 0.45
    }
}
