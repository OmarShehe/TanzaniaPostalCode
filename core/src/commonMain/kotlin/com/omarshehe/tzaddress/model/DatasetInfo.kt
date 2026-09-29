package com.omarshehe.tzaddress.model

public data class DatasetInfo(
    val version: String,
    val sourceEdition: String,
    /** ISO-8601 timestamp. */
    val generatedAt: String,
)
