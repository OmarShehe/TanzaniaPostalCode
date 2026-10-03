package com.omarshehe.tzaddress.importer

/** Text output of a ward-points import: a summary report and the list of wards left without a position. */
object WardPointsReport {
    private val zanzibarRegions = setOf("Mjini Magharibi", "Kusini Unguja", "Kaskazini Unguja", "Kusini Pemba", "Kaskazini Pemba")

    /** [problem] is the failure text when the run did not pass, or null. */
    fun report(dataset: DatasetDto, matches: List<WardMatch>, minRatio: Double, problem: String?, skippedBoundaryFeatures: Int = 0): String {
        val byPostcode = matches.associateBy { it.postcode }
        val regions = dataset.regions.sortedBy { it.code }
        fun wardsOf(region: RegionDto) = region.districts.flatMap { it.wards }
        fun placed(region: RegionDto) = wardsOf(region).count { byPostcode[it.postcode]?.point != null }
        val total = regions.sumOf { wardsOf(it).size }
        val withPoint = matches.count { it.point != null }
        val ratio = if (total == 0) 0.0 else withPoint.toDouble() / total
        val zanzibar = regions.filter { it.name in zanzibarRegions }

        return buildString {
            appendLine("# Ward positions report")
            appendLine()
            appendLine("**Result: ${if (problem == null) "PASSED" else "FAILED"}**")
            if (problem != null) {
                appendLine()
                appendLine(problem)
            }
            appendLine()
            appendLine("- Wards: $total")
            appendLine("- With a position: $withPoint (%.3f; minimum %.3f)".format(ratio, minRatio))
            WardMatchKind.entries.forEach { kind -> appendLine("- ${kind.name}: ${matches.count { it.kind == kind }}") }
            if (skippedBoundaryFeatures > 0) appendLine("- Boundary features skipped (no name or no usable geometry): $skippedBoundaryFeatures")
            appendLine("- Zanzibar wards with a position: ${zanzibar.sumOf { placed(it) }} of ${zanzibar.sumOf { wardsOf(it).size }}")
            appendLine()
            appendLine("## Per region")
            appendLine()
            appendLine("| Code | Region | Wards | With a position | Without |")
            appendLine("|---|---|---|---|---|")
            regions.forEach { appendLine("| ${it.code} | ${it.name} | ${wardsOf(it).size} | ${placed(it)} | ${wardsOf(it).size - placed(it)} |") }
            val similar = dataset.regions.flatMap { r -> r.districts.flatMap { d -> d.wards.map { w -> Pair(d, w) } } }
                .sortedBy { (_, ward) -> ward.postcode }
                .mapNotNull { (district, ward) -> byPostcode[ward.postcode]?.takeIf { it.kind == WardMatchKind.MATCHED_SIMILAR_NAME }?.let { "- ${ward.postcode} ${ward.name} (${district.name}): ${it.detail}" } }
            if (similar.isNotEmpty()) {
                appendLine()
                appendLine("## Similar-name matches")
                appendLine()
                similar.forEach { appendLine(it) }
            }
        }
    }

    fun anomaliesCsv(dataset: DatasetDto, matches: List<WardMatch>): String {
        val byPostcode = matches.associateBy { it.postcode }
        return buildString {
            appendLine("postcode,region,district,ward,kind,detail")
            dataset.regions.flatMap { r -> r.districts.flatMap { d -> d.wards.map { Triple(r, d, it) } } }
                .sortedBy { (_, _, ward) -> ward.postcode }
                .forEach { (region, district, ward) ->
                    val match = byPostcode[ward.postcode] ?: return@forEach
                    if (match.point == null) {
                        appendLine("${ward.postcode},${csv(region.name)},${csv(district.name)},${csv(ward.name)},${match.kind},\"${match.detail.replace("\"", "\"\"")}\"")
                    }
                }
        }
    }

    private fun csv(value: String) = if (value.any { it == ',' || it == '"' }) "\"${value.replace("\"", "\"\"")}\"" else value
}
