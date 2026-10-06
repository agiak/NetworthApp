package com.agcoding.networkapp.shared.ui.utils

/** Accepts both "." and "," as decimal separator, keeps at most one separator and [maxDecimals] decimals. */
fun sanitizeAmountInput(raw: String, maxDecimals: Int = 2): String {
    val normalized = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dotIdx = normalized.indexOf('.')
    if (dotIdx < 0) return normalized
    val intPart = normalized.substring(0, dotIdx)
    val decPart = normalized.substring(dotIdx + 1).replace(".", "").take(maxDecimals)
    return "$intPart.$decPart"
}
