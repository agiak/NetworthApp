package com.agcoding.networkapp.expenseanalysis.domain.categorizer

import java.text.Normalizer
import java.util.Locale

/**
 * Turns an expense name into comparable tokens: lower case, no accents, Greek letters
 * spelled phonetically in Latin ("Ρεύμα" → "revma", "Νέτφλιξ" → "netflix"), so Greek,
 * English and Greeklish names meet in the same alphabet.
 */
object TextNormalizer {

    private const val DIAERESIS = '̈'

    /** Accents and other marks, except the diaeresis which splits Greek digraphs (αϊ ≠ αι). */
    private val marksExceptDiaeresis = Regex("[\\p{M}&&[^\\u0308]]+")
    private val separators = Regex("[^\\p{L}\\p{N}\\u0308]+")

    private val digraphs = mapOf(
        "ου" to "u", "αυ" to "av", "ευ" to "ev", "αι" to "e", "ει" to "i", "οι" to "i",
        "μπ" to "b", "ντ" to "d", "γκ" to "g", "γγ" to "ng", "τσ" to "ts", "τζ" to "tz",
    )

    private val letters = mapOf(
        'α' to "a", 'β' to "v", 'γ' to "g", 'δ' to "d", 'ε' to "e", 'ζ' to "z", 'η' to "i",
        'θ' to "th", 'ι' to "i", 'κ' to "k", 'λ' to "l", 'μ' to "m", 'ν' to "n", 'ξ' to "x",
        'ο' to "o", 'π' to "p", 'ρ' to "r", 'σ' to "s", 'ς' to "s", 'τ' to "t", 'υ' to "i",
        'φ' to "f", 'χ' to "h", 'ψ' to "ps", 'ω' to "o",
    )

    /** Words of [text] with two characters or more (numbers included). */
    fun tokens(text: String): List<String> =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(marksExceptDiaeresis, "")
            .split(separators)
            .map(::toLatin)
            .filter { it.length >= MIN_TOKEN_LENGTH }

    /** Stable key for an expense name, used to remember the user's choice for it. */
    fun key(text: String): String = tokens(text).joinToString(" ")

    internal fun toLatin(token: String): String {
        val out = StringBuilder(token.length + 4)
        var i = 0
        while (i < token.length) {
            val c = token[i]
            if (c == DIAERESIS) { i++; continue }
            // A diaeresis on the second letter keeps the two vowels apart: "αϊ" → "ai"
            val pair = if (i + 1 < token.length && token.getOrNull(i + 2) != DIAERESIS) token.substring(i, i + 2) else null
            val digraph = pair?.let { digraphs[it] }
            if (digraph != null) {
                out.append(digraph)
                i += 2
            } else {
                out.append(letters[c] ?: c.toString())
                i++
            }
        }
        return out.toString()
    }

    private const val MIN_TOKEN_LENGTH = 2
}
