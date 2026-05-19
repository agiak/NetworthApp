package com.agcoding.networkapp.account.domain.model

data class Account(
    val id: Long = 0,
    val name: String,
    val startingBalance: Double = 0.0,
    val colorHex: String = "#76C893",
) {
    companion object {
        val PRESET_COLORS = listOf(
            "#76C893", "#5B8DEF", "#FF8C69", "#A78BFA", "#F59E0B", "#EC4899",
        )
    }
}
