package com.omarshehe.tzaddress.model

public data class Ward(
    /** Five-digit postcode; globally unique and the ward identifier. */
    val postcode: String,
    val name: String,
    val districtCode: String,
    /** Position inside the ward for centring a map, not a property location; null when unknown. Both or neither. */
    val latitude: Double? = null,
    val longitude: Double? = null,
)
