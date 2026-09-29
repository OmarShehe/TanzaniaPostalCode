package com.omarshehe.tzaddress

/** One search suggestion. [postcode] is the ward postcode of the nearest ancestor that has one; null above ward level. */
public data class AddressMatch(
    val path: AddressPath,
    val level: Level,
    val label: String,
    val postcode: String?,
    val score: Double,
)
