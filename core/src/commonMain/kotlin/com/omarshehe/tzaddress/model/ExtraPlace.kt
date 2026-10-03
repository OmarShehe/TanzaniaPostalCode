package com.omarshehe.tzaddress.model

import com.omarshehe.tzaddress.Level
import kotlin.jvm.JvmOverloads

/**
 * A place an app adds beside the bundled dataset: a ward, mtaa/village or kitongoji.
 *
 * [parentId] is a district code for a ward, a ward postcode for an mtaa and an mtaa id for a kitongoji. A ward needs a
 * five-digit [postcode] that starts with its district code; the other levels have none. [latitude] and [longitude]
 * (wards only, both or neither) place the ward on a map.
 */
public data class ExtraPlace @JvmOverloads constructor(
    val level: Level,
    val name: String,
    val parentId: String,
    val postcode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
