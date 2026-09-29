package com.omarshehe.tzaddress.ui

import com.omarshehe.tzaddress.AddressMatch
import com.omarshehe.tzaddress.Level

/** `Name — District, Region`; mtaa and kitongoji also name their ward, since names repeat across wards. */
internal fun AddressMatch.suggestionTitle(): String {
    val where = buildList {
        if (level > Level.WARD) path.ward?.let { add(it.name) }
        path.district?.let { add(it.name) }
        add(path.region.name)
    }
    return if (level == Level.REGION) label else "$label — ${where.joinToString(", ")}"
}
