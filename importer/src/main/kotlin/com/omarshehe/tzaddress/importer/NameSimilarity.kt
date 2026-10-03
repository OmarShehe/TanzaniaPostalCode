package com.omarshehe.tzaddress.importer

/** How alike two name keys are, as 1 minus the edit distance over the longer length; names that start with different letters never count. */
object NameSimilarity {

    fun score(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty() || a.first() != b.first()) return 0.0
        return 1.0 - distance(a, b).toDouble() / maxOf(a.length, b.length)
    }

    private fun distance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(substitution, previous[j] + 1, current[j - 1] + 1)
            }
            previous = current
        }
        return previous[b.length]
    }
}
