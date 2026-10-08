package com.agcoding.networkapp.expenseanalysis.domain.model

enum class ExpenseCategory {
    SUBSCRIPTIONS,
    HOUSING,
    TRANSPORT,
    DINING_LEISURE,
    TRAVEL,
    GROCERIES,
    HEALTH,
    FINANCE,
    EDUCATION,
    SHOPPING_PERSONAL,
    KIDS_PETS,
    OTHER;

    companion object {
        fun fromName(name: String): ExpenseCategory? = entries.find { it.name == name }
    }
}
