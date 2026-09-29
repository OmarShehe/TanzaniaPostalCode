package com.omarshehe.tzaddress.model

public data class Ward(
    /** Five-digit postcode; globally unique and the ward identifier. */
    val postcode: String,
    val name: String,
    val districtCode: String,
)
