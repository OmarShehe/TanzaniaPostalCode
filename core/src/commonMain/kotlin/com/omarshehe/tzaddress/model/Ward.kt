package com.omarshehe.tzaddress.model

import kotlin.jvm.JvmOverloads

public data class Ward @JvmOverloads constructor(
    /** Five-digit postcode; globally unique and the ward identifier. */
    val postcode: String,
    val name: String,
    val districtCode: String,
    /** Position inside the ward for centring a map, not a property location; null when unknown. Both or neither. */
    val latitude: Double? = null,
    val longitude: Double? = null,
)
