package com.omarshehe.tzaddress.importer

/** Failure policy; both values are configurable in `importer/gradle.properties`. */
data class Policy(val expectedRegions: Int, val maxAnomalyRatio: Double)
