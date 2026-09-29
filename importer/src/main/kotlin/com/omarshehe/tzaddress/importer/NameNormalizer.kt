package com.omarshehe.tzaddress.importer

/** Cleans names as printed in the source. Only all-upper or all-lower words are re-cased; mixed-case words are trusted. */
object NameNormalizer {
    private val connectors = setOf("na", "wa", "ya", "la", "es")
    private val acronyms = setOf("CBD")
    private val roman = Regex("^[IVX]{2,4}$")
    private val whitespace = Regex("\\s+")
    private val typographicHyphens = Regex("[\\u2010-\\u2015\\u2212]")

    fun normalize(raw: String): String =
        raw.replace('\u2019', '\'').replace('\u2018', '\'').replace('`', '\'')
            .replace('\u201C', '"').replace('\u201D', '"')
            .replace(typographicHyphens, "-")
            .trim().replace(whitespace, " ")
            .split(" ").filter { it.isNotEmpty() }
            .mapIndexed { index, token -> token(token, index) }
            .joinToString(" ")

    private fun token(token: String, index: Int): String {
        val letters = token.filter { it.isLetter() }
        return when {
            token.any { it.isDigit() } -> token
            letters.length <= 1 -> token.map { if (it.isLetter()) it.uppercaseChar() else it }.joinToString("")
            token.uppercase() in acronyms -> token.uppercase()
            roman.matches(letters) && letters == letters.uppercase() -> token
            index > 0 && token.lowercase() in connectors -> token.lowercase()
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
