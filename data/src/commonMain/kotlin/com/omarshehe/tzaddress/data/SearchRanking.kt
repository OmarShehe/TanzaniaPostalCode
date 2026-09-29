package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.AddressText
import com.omarshehe.tzaddress.Level

/** Score: exact name 4, name prefix 3, otherwise the share of query tokens that prefix a name token (0..1). */
internal object SearchRanking {

    fun score(query: String, name: String): Double {
        val q = AddressText.normalize(query)
        val n = AddressText.normalize(name)
        if (q.isEmpty()) return 0.0
        if (q == n) return 4.0
        if (n.startsWith(q)) return 3.0
        val nameTokens = n.split(' ')
        val queryTokens = q.split(' ')
        return queryTokens.count { t -> nameTokens.any { it.startsWith(t) } }.toDouble() / queryTokens.size
    }

    class Ranked(val score: Double, val level: Level, val name: String, val refId: String)

    /** Higher score first, then higher level (region before kitongoji), then name, then id: fully deterministic. */
    val order: Comparator<Ranked> =
        compareByDescending<Ranked> { it.score }.thenBy { it.level.ordinal }.thenBy { it.name }.thenBy { it.refId }
}
