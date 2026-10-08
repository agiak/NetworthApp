package com.agcoding.networkapp.expenseanalysis.domain.categorizer

import com.agcoding.networkapp.expenseanalysis.domain.model.CategoryFeedback
import com.agcoding.networkapp.expenseanalysis.domain.model.CategoryPrediction
import com.agcoding.networkapp.expenseanalysis.domain.model.CategorySource
import com.agcoding.networkapp.expenseanalysis.domain.model.ExpenseCategory
import kotlin.math.min

/**
 * Guesses the category of an expense from its name (and, more weakly, its note).
 *
 * 1. A name the user has categorised before always gets the user's category.
 * 2. Otherwise every word scores the categories it hints at, from two sources:
 *    - the built-in [SeedKeywords] dictionary (with stems and small typos),
 *    - the words of names the user has categorised: each choice teaches the categoriser,
 *      and the more choices agree on a word, the stronger it gets — enough to overrule the
 *      dictionary after a couple of corrections.
 * 3. The highest score wins; nothing matching means [ExpenseCategory.OTHER].
 */
class ExpenseCategorizer(feedback: List<CategoryFeedback> = emptyList()) {

    private val manual: Map<String, ExpenseCategory> = feedback.associate { it.nameKey to it.category }

    /** word → how many of the user's choices containing it went to each category */
    private val learned: Map<String, Map<ExpenseCategory, Int>> = buildMap<String, MutableMap<ExpenseCategory, Int>> {
        feedback.forEach { choice ->
            choice.nameKey.split(' ').filter(::isLearnable).distinct().forEach { word ->
                val counts = getOrPut(word) { mutableMapOf() }
                counts[choice.category] = (counts[choice.category] ?: 0) + 1
            }
        }
    }

    fun categorize(title: String, note: String = ""): CategoryPrediction {
        manual[TextNormalizer.key(title)]?.let { return CategoryPrediction(it, 1.0, CategorySource.MANUAL) }

        val categories = ExpenseCategory.entries
        val seed = DoubleArray(categories.size)
        val fromUser = DoubleArray(categories.size)

        val titleTokens = TextNormalizer.tokens(title)
        val noteTokens = TextNormalizer.tokens(note)
        addSeedScores(titleTokens, seed, 1.0)
        addSeedScores(noteTokens, seed, NOTE_FACTOR)
        addLearnedScores(titleTokens, fromUser, 1.0)
        addLearnedScores(noteTokens, fromUser, NOTE_FACTOR)

        val totals = DoubleArray(categories.size) { seed[it] + fromUser[it] }
        val ranked = totals.indices.sortedByDescending { totals[it] }
        val best = ranked[0]
        val bestScore = totals[best]
        if (bestScore <= 0.0) return CategoryPrediction(ExpenseCategory.OTHER, 0.0, CategorySource.NONE)

        val runnerUp = totals[ranked[1]]
        val margin = (bestScore - runnerUp) / bestScore
        val confidence = min(1.0, bestScore / CONFIDENT_SCORE) * (0.5 + 0.5 * margin)
        val source = if (fromUser[best] >= seed[best]) CategorySource.LEARNED else CategorySource.AUTO
        return CategoryPrediction(categories[best], confidence, source)
    }

    private fun addSeedScores(tokens: List<String>, scores: DoubleArray, factor: Double) {
        if (tokens.isEmpty()) return
        SeedKeywords.all.forEach { keyword ->
            val match = bestMatch(tokens, keyword)
            if (match > 0.0) {
                val phraseBonus = 1.0 + PHRASE_BONUS * (keyword.parts.size - 1)
                scores[keyword.category.ordinal] += keyword.weight * phraseBonus * match * factor
            }
        }
    }

    private fun addLearnedScores(tokens: List<String>, scores: DoubleArray, factor: Double) {
        tokens.filter(::isLearnable).distinct().forEach { word ->
            val counts = learned[word] ?: return@forEach
            val total = counts.values.sum()
            // count / (total + 1): one example gives half the weight, agreement makes it grow,
            // disagreement between categories splits it
            counts.forEach { (category, count) ->
                scores[category.ordinal] += LEARNED_WEIGHT * count / (total + 1.0) * factor
            }
        }
    }

    /** Best match of [keyword] anywhere in [tokens], 0.0 (none) to 1.0 (exact). */
    private fun bestMatch(tokens: List<String>, keyword: SeedKeyword): Double {
        val parts = keyword.parts
        if (tokens.size < parts.size) return 0.0
        var best = 0.0
        for (start in 0..tokens.size - parts.size) {
            var phraseMatch = 1.0
            for (j in parts.indices) {
                phraseMatch = min(phraseMatch, partMatch(tokens[start + j], parts[j], keyword.exact))
                if (phraseMatch == 0.0) break
            }
            if (phraseMatch > best) best = phraseMatch
            if (best == 1.0) break
        }
        return best
    }

    private fun partMatch(token: String, part: String, exact: Boolean): Double {
        if (token == part) return 1.0
        if (exact) return 0.0
        if (part.length >= MIN_STEM_LENGTH && token.startsWith(part)) return STEM_MATCH
        if (part.length < MIN_FUZZY_LENGTH || token.length < MIN_FUZZY_LENGTH) return 0.0
        val maxTypos = if (part.length >= LONG_WORD_LENGTH) 2 else 1
        if (levenshtein(token, part, maxTypos) <= maxTypos) return TYPO_MATCH
        // Stem with a typo: "benzinis" ~ "venzin"
        if (token.length > part.length && levenshtein(token.take(part.length), part, 1) <= 1) return STEM_TYPO_MATCH
        return 0.0
    }

    private fun isLearnable(word: String): Boolean =
        word.length >= 2 && word !in STOP_WORDS && !word.all(Char::isDigit)

    companion object {
        private const val NOTE_FACTOR = 0.5
        private const val PHRASE_BONUS = 0.75
        private const val LEARNED_WEIGHT = 4.0
        /** Score at which a guess counts as fully confident. */
        private const val CONFIDENT_SCORE = 3.0

        private const val MIN_STEM_LENGTH = 4
        private const val MIN_FUZZY_LENGTH = 5
        private const val LONG_WORD_LENGTH = 8

        private const val STEM_MATCH = 0.85
        private const val TYPO_MATCH = 0.6
        private const val STEM_TYPO_MATCH = 0.5

        /** Words too common to teach anything. */
        private val STOP_WORDS: Set<String> = listOf(
            "the", "of", "my", "for", "and", "to", "in", "on", "per", "at", "with", "from", "our",
            "monthly", "yearly", "annual", "month", "year", "fee", "bill", "new", "old",
            "με", "το", "τα", "τη", "την", "της", "του", "των", "τον", "οι", "για", "και", "σε",
            "στο", "στη", "στην", "στα", "μου", "μας", "σου", "απο", "μηνιαιο", "μηνιαια",
            "ετησιο", "ετησια", "μηνα", "χρονο", "λογαριασμος", "πληρωμη", "payment",
        ).flatMap(TextNormalizer::tokens).toSet()

        /** Edit distance, giving up (returning max + 1) once it exceeds [max]. */
        internal fun levenshtein(a: String, b: String, max: Int): Int {
            if (kotlin.math.abs(a.length - b.length) > max) return max + 1
            var previous = IntArray(b.length + 1) { it }
            var current = IntArray(b.length + 1)
            for (i in 1..a.length) {
                current[0] = i
                var rowMin = current[0]
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
                    if (current[j] < rowMin) rowMin = current[j]
                }
                if (rowMin > max) return max + 1
                val swap = previous; previous = current; current = swap
            }
            return previous[b.length]
        }
    }
}
