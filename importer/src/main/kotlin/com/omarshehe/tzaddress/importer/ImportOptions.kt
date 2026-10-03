package com.omarshehe.tzaddress.importer

import java.io.File

/** Command-line options as `--key=value`; `--force` is a boolean (`--force`, `--force=true`, `--force=false`). */
class ImportOptions(
    val pdf: File?,
    val pdfDir: File?,
    val outDir: File,
    val sourceEdition: String?,
    val generatedAt: String?,
    val policy: Policy,
    val force: Boolean,
) {
    companion object {
        const val DEFAULT_EXPECTED_REGIONS = 30
        const val DEFAULT_EXPECTED_REGIONS_WITH_REGION_FILES = 31
        const val DEFAULT_MAX_ANOMALY_RATIO = 0.005
        private val FALSE_VALUES = setOf("false", "0", "no")

        fun parse(args: Array<String>): ImportOptions {
            val values = args.filter { it.startsWith("--") }.associate { arg ->
                val body = arg.removePrefix("--")
                val at = body.indexOf('=')
                if (at < 0) body to "" else body.substring(0, at) to body.substring(at + 1)
            }
            val root = File(values["root"] ?: ".")
            val outDir = File(values["outDir"] ?: "dataset").let { if (it.isAbsolute) it else File(root, it.path) }
            val pdfDir = values["pdfDir"]?.takeIf { it.isNotBlank() }?.let(::File)
            return ImportOptions(
                pdf = values["pdf"]?.takeIf { it.isNotBlank() }?.let(::File),
                pdfDir = pdfDir,
                outDir = outDir,
                sourceEdition = values["sourceEdition"]?.takeIf { it.isNotBlank() },
                generatedAt = values["generatedAt"]?.takeIf { it.isNotBlank() },
                policy = Policy(
                    expectedRegions = values["expectedRegions"]?.let { number("expectedRegions", it, String::toIntOrNull) } ?: if (pdfDir != null) DEFAULT_EXPECTED_REGIONS_WITH_REGION_FILES else DEFAULT_EXPECTED_REGIONS,
                    maxAnomalyRatio = values["maxAnomalyRatio"]?.let { number("maxAnomalyRatio", it, String::toDoubleOrNull) } ?: DEFAULT_MAX_ANOMALY_RATIO,
                ),
                force = values["force"]?.let { it.lowercase() !in FALSE_VALUES } ?: false,
            )
        }

        private fun <T> number(option: String, value: String, convert: (String) -> T?): T =
            convert(value) ?: throw IllegalArgumentException("Invalid value for -P$option: '$value' is not a number")
    }
}
