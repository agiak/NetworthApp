package com.agcoding.networkapp.expenseanalysis.presentation.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.agcoding.networkapp.R
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory

val ExpenseCategory.emoji: String
    get() = when (this) {
        ExpenseCategory.SUBSCRIPTIONS     -> "🔁"
        ExpenseCategory.HOUSING           -> "🏠"
        ExpenseCategory.TRANSPORT         -> "🚗"
        ExpenseCategory.DINING_LEISURE    -> "🍽️"
        ExpenseCategory.TRAVEL            -> "✈️"
        ExpenseCategory.GROCERIES         -> "🛒"
        ExpenseCategory.HEALTH            -> "🩺"
        ExpenseCategory.FINANCE           -> "🏦"
        ExpenseCategory.EDUCATION         -> "📚"
        ExpenseCategory.SHOPPING_PERSONAL -> "🛍️"
        ExpenseCategory.KIDS_PETS         -> "🧸"
        ExpenseCategory.OTHER             -> "📦"
    }

@get:StringRes
val ExpenseCategory.labelRes: Int
    get() = when (this) {
        ExpenseCategory.SUBSCRIPTIONS     -> R.string.expense_category_subscriptions
        ExpenseCategory.HOUSING           -> R.string.expense_category_housing
        ExpenseCategory.TRANSPORT         -> R.string.expense_category_transport
        ExpenseCategory.DINING_LEISURE    -> R.string.expense_category_dining_leisure
        ExpenseCategory.TRAVEL            -> R.string.expense_category_travel
        ExpenseCategory.GROCERIES         -> R.string.expense_category_groceries
        ExpenseCategory.HEALTH            -> R.string.expense_category_health
        ExpenseCategory.FINANCE           -> R.string.expense_category_finance
        ExpenseCategory.EDUCATION         -> R.string.expense_category_education
        ExpenseCategory.SHOPPING_PERSONAL -> R.string.expense_category_shopping_personal
        ExpenseCategory.KIDS_PETS         -> R.string.expense_category_kids_pets
        ExpenseCategory.OTHER             -> R.string.expense_category_other
    }

val ExpenseCategory.color: Color
    get() = when (this) {
        ExpenseCategory.SUBSCRIPTIONS     -> Color(0xFFA78BFA)
        ExpenseCategory.HOUSING           -> Color(0xFF5B8DEF)
        ExpenseCategory.TRANSPORT         -> Color(0xFFFF8C69)
        ExpenseCategory.DINING_LEISURE    -> Color(0xFFEC4899)
        ExpenseCategory.TRAVEL            -> Color(0xFF22B8CF)
        ExpenseCategory.GROCERIES         -> Color(0xFF76C893)
        ExpenseCategory.HEALTH            -> Color(0xFFEF4444)
        ExpenseCategory.FINANCE           -> Color(0xFFF59E0B)
        ExpenseCategory.EDUCATION         -> Color(0xFF6366F1)
        ExpenseCategory.SHOPPING_PERSONAL -> Color(0xFFD946EF)
        ExpenseCategory.KIDS_PETS         -> Color(0xFF84CC16)
        ExpenseCategory.OTHER             -> Color(0xFF94A3B8)
    }
