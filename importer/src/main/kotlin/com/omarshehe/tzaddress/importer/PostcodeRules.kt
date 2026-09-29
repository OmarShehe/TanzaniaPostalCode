package com.omarshehe.tzaddress.importer

/** The one place that defines what a valid postcode looks like. */
object PostcodeRules {
    val ward = Regex("^\\d{5}$")
    val district = Regex("^\\d{2,3}$")
}
