package com.omarshehe.tzaddress

/**
 * Text normalisation used for matching: the dataset generator and the query side must agree,
 * so both call this. Lower-case, apostrophes dropped, diacritics stripped, other punctuation to spaces.
 */
public object AddressText {

    public fun normalize(text: String): String {
        val out = StringBuilder(text.length)
        for (ch in text.lowercase()) {
            when {
                ch == '\'' || ch == '’' || ch == '`' || ch == '‘' -> Unit
                ch in '0'..'9' || ch in 'a'..'z' -> out.append(ch)
                ch in DIACRITICS -> out.append(DIACRITICS.getValue(ch))
                ch.isLetterOrDigit() -> out.append(ch)
                else -> out.append(' ')
            }
        }
        return out.toString().trim().replace(SPACES, " ")
    }

    public fun tokens(text: String): List<String> =
        normalize(text).takeIf { it.isNotEmpty() }?.split(' ') ?: emptyList()

    private val SPACES = Regex(" +")

    private val DIACRITICS: Map<Char, Char> = buildMap {
        "àáâãäå" .forEach { put(it, 'a') }
        "èéêë".forEach { put(it, 'e') }
        "ìíîï".forEach { put(it, 'i') }
        "òóôõö".forEach { put(it, 'o') }
        "ùúûü".forEach { put(it, 'u') }
        put('ç', 'c'); put('ñ', 'n')
    }
}
