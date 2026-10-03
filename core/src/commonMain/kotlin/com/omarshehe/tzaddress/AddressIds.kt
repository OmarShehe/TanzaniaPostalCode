package com.omarshehe.tzaddress

/**
 * Deterministic ids for levels that have no postcode in the source (mtaa/village, kitongoji).
 * Id = `<parentId>/<slug(name)>`; duplicate names under one parent get `-2`, `-3`, ... in encounter order.
 */
public object AddressIds {

    public fun slug(name: String): String {
        val cleaned = name.lowercase()
            .filterNot { it == '\'' || it == '"' || it == '’' }
            .replace(NON_ALPHANUMERIC, "-")
            .trim('-')
        return cleaned.ifEmpty { "unnamed" }
    }

    public fun mtaaIds(wardPostcode: String, names: List<String>): List<String> =
        childIds(wardPostcode, names)

    public fun kitongojiIds(mtaaId: String, names: List<String>): List<String> =
        childIds(mtaaId, names)

    private fun childIds(parentId: String, names: List<String>): List<String> {
        val used = HashSet<String>()
        return names.map { name ->
            val base = "$parentId/${slug(name)}"
            var candidate = base
            var n = 2
            while (!used.add(candidate)) candidate = "$base-${n++}"
            candidate
        }
    }

    private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")
}
