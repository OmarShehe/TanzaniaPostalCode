package com.omarshehe.tzaddress.importer

/**
 * Cleans names as printed in the source. Names printed in capitals are title-cased, unless they are short
 * (acronyms such as CCM or NHC); all-lower words are capitalised; mixed-case words and acronyms inside
 * mixed-case names are trusted.
 */
object NameNormalizer {
    private val connectors = setOf("na", "wa", "ya", "la", "es")
    /** Acronyms that are always upper-case. Standalone all-caps acronyms cannot be told from shouted words by rule, so add them here. */
    private val acronyms = setOf("CBD", "TANESCO")
    private const val SHOUTING_WORD_LENGTH = 4
    private val roman = Regex("^[IVX]{2,4}$")
    private val whitespace = Regex("\\s+")
    private val typographicHyphens = Regex("[\\u2010-\\u2015\\u2212]")

    /**
     * [shortAllCapsAreAcronyms] applies to mtaa and kitongoji names, where a short all-capital name is an acronym
     * (CCM). Wards and districts are sometimes printed entirely in capitals as ordinary words (HAI, KIA), so there it stays off.
     */
    fun normalize(raw: String, shortAllCapsAreAcronyms: Boolean = false): String =
        raw.replace('\u2019', '\'').replace('\u2018', '\'').replace('`', '\'').replace('\u00B4', '\'')
            .trimEnd().removeSuffix("*")
            .replace('\u201C', '"').replace('\u201D', '"')
            .replace(typographicHyphens, "-")
            .trim().replace(whitespace, " ")
            .let { cleaned ->
                val shouting = isShouting(cleaned, shortAllCapsAreAcronyms)
                cleaned.split(" ").filter { it.isNotEmpty() }
                    .mapIndexed { index, token -> token(token, index, shouting) }
                    .joinToString(" ")
            }

    /**
     * A name printed with no lowercase letters at all. When [acronymLevel] is set, such a name is only "shouting"
     * if it contains a long word or a connector (wa, ya, ...); otherwise it is an acronym (CCM, NHC).
     */
    private fun isShouting(name: String, acronymLevel: Boolean): Boolean {
        val letters = name.filter { it.isLetter() }
        if (letters != letters.uppercase()) return false
        if (!acronymLevel) return true
        val words = name.split(" ")
        return words.any { it.count { c -> c.isLetter() } >= SHOUTING_WORD_LENGTH } ||
            words.drop(1).any { it.lowercase() in connectors }
    }

    private fun token(token: String, index: Int, shouting: Boolean): String {
        val letters = token.filter { it.isLetter() }
        return when {
            token.any { it.isDigit() } -> token
            letters.length <= 1 -> token.map { if (it.isLetter()) it.uppercaseChar() else it }.joinToString("")
            token.uppercase() in acronyms -> token.uppercase()
            roman.matches(letters) && letters == letters.uppercase() -> token
            index > 0 && token.lowercase() in connectors -> token.lowercase()
            !shouting && letters == letters.uppercase() -> token
            letters != letters.uppercase() && letters != letters.lowercase() -> token
            else -> capitalizeParts(token.lowercase())
        }
    }

    private fun capitalizeParts(lower: String): String {
        val out = StringBuilder(lower.length)
        var capitalizeNext = true
        for (c in lower) {
            if (capitalizeNext && c.isLetter()) {
                out.append(c.uppercaseChar())
                capitalizeNext = false
            } else {
                out.append(c)
                if (c == '-' || c == '/' || c == '"' || c == '(') capitalizeNext = true
                else if (c.isLetter()) capitalizeNext = false
            }
        }
        return out.toString()
    }
}
