package com.omarshehe.tzaddress.importer

import java.io.File

/** Command-line options of the ward-points import, as `--key=value`. */
class WardPointsOptions(
    val wardBoundaries: File?,
    val districtBoundaries: File?,
    val outDir: File,
    val minMatchRatio: Double,
    val generatedAt: String?,
) {
    companion object {
        fun parse(args: Array<String>): WardPointsOptions {
            val values = args.filter { it.startsWith("--") }.associate { arg ->
                val body = arg.removePrefix("--")
                val at = body.indexOf('=')
                if (at < 0) body to "" else body.substring(0, at) to body.substring(at + 1)
            }
            val root = File(values["root"] ?: ".")
            val outDir = File(values["outDir"] ?: "dataset").let { if (it.isAbsolute) it else File(root, it.path) }
            return WardPointsOptions(
                wardBoundaries = values["wardBoundaries"]?.takeIf { it.isNotBlank() }?.let(::File),
                districtBoundaries = values["districtBoundaries"]?.takeIf { it.isNotBlank() }?.let(::File),
                outDir = outDir,
                minMatchRatio = values["minMatchRatio"]?.let {
                    it.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid value for -PminMatchRatio: '$it' is not a number")
                } ?: WardPointValidator.MIN_MATCH_RATIO,
                generatedAt = values["generatedAt"]?.takeIf { it.isNotBlank() },
            )
        }
    }
}
