package com.agcoding.networkapp.goal.presentation

enum class GoalTimeframe {
    ONE_YEAR, TWO_YEARS, THREE_YEARS, FIVE_YEARS, CUSTOM;

    val fixedMonths: Int? get() = when (this) {
        ONE_YEAR -> 12
        TWO_YEARS -> 24
        THREE_YEARS -> 36
        FIVE_YEARS -> 60
        CUSTOM -> null
    }
}
