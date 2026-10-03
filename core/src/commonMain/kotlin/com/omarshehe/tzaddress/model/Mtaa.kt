package com.omarshehe.tzaddress.model

public data class Mtaa(
    /** Deterministic id from [com.omarshehe.tzaddress.AddressIds]; the source has no postcode at this level. */
    val id: String,
    val name: String,
    val wardPostcode: String,
)
