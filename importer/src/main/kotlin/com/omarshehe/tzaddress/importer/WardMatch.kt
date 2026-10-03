package com.omarshehe.tzaddress.importer

/** The outcome of the join for one ward; [point] is set exactly when [kind] is a match. */
data class WardMatch(val postcode: String, val kind: WardMatchKind, val point: WardPoint?, val detail: String)
