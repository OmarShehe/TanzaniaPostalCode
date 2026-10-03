package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.AddressText

/** Turns user text into a safe FTS5 MATCH expression. */
internal object SearchQuery {
    const val MAX_LIMIT = 100

    /** `"tok1"* "tok2"*` (AND of prefix matches), or null when nothing searchable remains. Tokens hold no FTS syntax after normalisation. */
    fun match(query: String): String? =
        AddressText.tokens(query).takeIf { it.isNotEmpty() }?.joinToString(" ") { "\"$it\"*" }

    fun clampLimit(limit: Int): Int = limit.coerceIn(1, MAX_LIMIT)
}
