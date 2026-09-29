package com.omarshehe.tzaddress.importer

/** Row counts per level. */
data class AddressDbCounts(val regions: Int, val districts: Int, val wards: Int, val mtaas: Int, val kitongojis: Int) {

    /** Fails naming every level whose count differs from [expected]. */
    fun verifyEquals(expected: AddressDbCounts, expectedFrom: String) {
        val diffs = buildList {
            if (regions != expected.regions) add("regions $regions != ${expected.regions}")
            if (districts != expected.districts) add("districts $districts != ${expected.districts}")
            if (wards != expected.wards) add("wards $wards != ${expected.wards}")
            if (mtaas != expected.mtaas) add("mtaa $mtaas != ${expected.mtaas}")
            if (kitongojis != expected.kitongojis) add("kitongoji $kitongojis != ${expected.kitongojis}")
        }
        check(diffs.isEmpty()) { "Database counts differ from $expectedFrom: ${diffs.joinToString("; ")}" }
    }

    companion object {
        /** Reads the totals block of `import-report.md`. */
        fun fromReport(report: String): AddressDbCounts {
            fun total(label: String): Int =
                Regex("^- ${Regex.escape(label)}: (\\d+)$", RegexOption.MULTILINE).find(report)?.groupValues?.get(1)?.toInt()
                    ?: error("import-report.md has no '$label' total")
            return AddressDbCounts(
                total("Regions"), total("Districts"), total("Wards"),
                total("Mtaa/village (incl. Zanzibar shehia)"), total("Kitongoji"),
            )
        }
    }
}
