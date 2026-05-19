package com.agcoding.networkapp.shared.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

fun formatWithSeparator(raw: String): String {
    if (raw.isEmpty()) return ""
    val dotIdx = raw.indexOf('.')
    val intPart = if (dotIdx >= 0) raw.substring(0, dotIdx) else raw
    val decPart = if (dotIdx >= 0) raw.substring(dotIdx) else ""
    if (intPart.isEmpty()) return raw
    val formattedInt = intPart.reversed().chunked(3).joinToString(",").reversed()
    return formattedInt + decPart
}

class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val dotIdx = raw.indexOf('.')
        val intPart = if (dotIdx >= 0) raw.substring(0, dotIdx) else raw
        val decPart = if (dotIdx >= 0) raw.substring(dotIdx) else ""
        val formattedInt = if (intPart.isEmpty()) ""
                           else intPart.reversed().chunked(3).joinToString(",").reversed()
        val formattedText = formattedInt + decPart
        val L = intPart.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val o = offset.coerceIn(0, raw.length)
                return if (o <= L) {
                    val commasBefore = if (L <= 1) 0
                        else (L - 1) / 3 - (L - o - 1).coerceAtLeast(0) / 3
                    o + commasBefore
                } else {
                    val commasTotal = if (L <= 1) 0 else (L - 1) / 3
                    L + commasTotal + (o - L)
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                val o = offset.coerceIn(0, formattedText.length)
                val commasInPrefix = formattedInt.take(o.coerceAtMost(formattedInt.length)).count { it == ',' }
                return (o - commasInPrefix).coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
