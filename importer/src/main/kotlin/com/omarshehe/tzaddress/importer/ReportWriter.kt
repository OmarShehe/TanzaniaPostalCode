package com.omarshehe.tzaddress.importer

object ReportWriter {
    private const val MAX_LISTED_ANOMALIES = 50
    private const val MAX_LISTED_WARNINGS = 30

    fun report(dataset: DatasetDto, validation: ValidationResult, anomalies: List<Anomaly>, dataLineCount: Int, datasetWritten: Boolean): String {
        val regions = dataset.regions.sortedBy { it.code }
        val districts = regions.sumOf { it.districts.size }
        val wards = regions.sumOf { r -> r.districts.sumOf { it.wards.size } }
        val mtaas = regions.sumOf { r -> r.districts.sumOf { d -> d.wards.sumOf { it.mtaas.size } } }
        val kitongojis = regions.sumOf { r -> r.districts.sumOf { d -> d.wards.sumOf { w -> w.mtaas.sumOf { it.kitongojis.size } } } }
        val ratio = if (dataLineCount == 0) 0.0 else anomalies.size.toDouble() / dataLineCount

        return buildString {
            appendLine("# Import report")
            appendLine()
            appendLine("**Result: ${if (validation.passed) "PASSED" else "FAILED"}**")
            appendLine()
            appendLine("- Dataset version: ${dataset.info.version}")
            appendLine("- Source edition: ${dataset.info.sourceEdition}")
            appendLine("- Generated at: ${dataset.info.generatedAt}")
            appendLine("- tz-address.json ${if (datasetWritten) "was written" else "was NOT written (validation failed); an existing copy is from an earlier run"}")
            appendLine()
            appendLine("## Totals")
            appendLine()
            appendLine("- Regions: ${regions.size}")
            appendLine("- Districts: $districts")
            appendLine("- Wards: $wards")
            appendLine("- Mtaa/village (incl. Zanzibar shehia): $mtaas")
            appendLine("- Kitongoji: $kitongojis")
            appendLine("- Source lines parsed: $dataLineCount")
            appendLine("- Anomalies: ${anomalies.size} (ratio ${"%.4f".format(ratio)})")
            appendLine()
            appendLine("## Per region")
            appendLine()
            appendLine("| Code | Region | Districts | Wards | Mtaa | Kitongoji |")
            appendLine("|---|---|---|---|---|---|")
            for (r in regions) {
                val w = r.districts.sumOf { it.wards.size }
                val m = r.districts.sumOf { d -> d.wards.sumOf { it.mtaas.size } }
                val k = r.districts.sumOf { d -> d.wards.sumOf { ward -> ward.mtaas.sumOf { it.kitongojis.size } } }
                appendLine("| ${r.code} | ${r.name} | ${r.districts.size} | $w | $m | $k |")
            }
            appendLine()
            appendLine("## Validation")
            appendLine()
            if (validation.violations.isEmpty()) appendLine("No violations.") else validation.violations.forEach { appendLine("- **${it.kind}**: ${it.message}") }
            if (validation.notes.isNotEmpty()) {
                appendLine()
                appendLine("### Notes")
                validation.notes.forEach { appendLine("- $it") }
            }
            if (validation.warnings.isNotEmpty()) {
                appendLine()
                appendLine("### Warnings (${validation.warnings.size})")
                validation.warnings.take(MAX_LISTED_WARNINGS).forEach { appendLine("- ${it.kind}: ${it.message}") }
                if (validation.warnings.size > MAX_LISTED_WARNINGS) appendLine("- ... ${validation.warnings.size - MAX_LISTED_WARNINGS} more")
            }
            appendLine()
            appendLine("## Anomalies")
            appendLine()
            if (anomalies.isEmpty()) appendLine("None.") else {
                appendLine("First ${minOf(anomalies.size, MAX_LISTED_ANOMALIES)} of ${anomalies.size} (all rows are in `import-anomalies.csv`):")
                appendLine()
                anomalies.take(MAX_LISTED_ANOMALIES).forEach { appendLine("- p${it.page} l${it.line} ${it.kind}: ${it.text}") }
            }
        }
    }

    fun anomaliesCsv(anomalies: List<Anomaly>): String = buildString {
        appendLine("page,line,kind,text")
        anomalies.forEach { appendLine("${it.page},${it.line},${it.kind},\"${it.text.replace("\"", "\"\"")}\"") }
    }
}
