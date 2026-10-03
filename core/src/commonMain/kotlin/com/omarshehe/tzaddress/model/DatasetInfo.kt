package com.omarshehe.tzaddress.model

import kotlin.jvm.JvmOverloads

public data class DatasetInfo @JvmOverloads constructor(
    val version: String,
    val sourceEdition: String,
    /** ISO-8601 timestamp. */
    val generatedAt: String,
    /** Data credit and licence notice to show with the data; empty when the dataset needs none. */
    val attribution: String = "",
)
