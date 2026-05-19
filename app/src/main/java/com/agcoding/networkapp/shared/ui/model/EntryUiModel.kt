package com.agcoding.networkapp.shared.ui.model

data class EntryUiModel(
    val id: Long,
    val formattedDate: String,
    val formattedValue: String,
    val accountColorHex: String = "",
    val accountName: String = "",
)
