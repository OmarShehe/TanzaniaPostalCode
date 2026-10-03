package com.omarshehe.tzaddress.importer

/** The usable features of a boundary file, and how many features were left out (no name, or no polygon geometry). */
class BoundaryFile(val features: List<BoundaryFeature>, val skipped: Int)
