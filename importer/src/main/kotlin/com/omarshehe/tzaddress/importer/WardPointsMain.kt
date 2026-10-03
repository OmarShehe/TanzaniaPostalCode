package com.omarshehe.tzaddress.importer

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val options = try {
        WardPointsOptions.parse(args)
    } catch (e: IllegalArgumentException) {
        System.err.println(e.message)
        exitProcess(2)
    }
    val wardFile = options.wardBoundaries
    val districtFile = options.districtBoundaries
    if (wardFile == null || districtFile == null || !wardFile.isFile || !districtFile.isFile) {
        System.err.println("Usage: ./gradlew :importer:importWardPoints -PwardBoundaries=/path/ward.geojson -PdistrictBoundaries=/path/district.geojson [-PminMatchRatio=0.85] [-PgeneratedAt=..] [-PoutDir=dataset]")
        System.err.println("Both -PwardBoundaries and -PdistrictBoundaries must point to existing files.")
        exitProcess(2)
    }
    val datasetFile = File(options.outDir, "tz-address.json")
    if (!datasetFile.isFile) {
        System.err.println("${datasetFile.path} not found; run :importer:importPostcodes first.")
        exitProcess(2)
    }

    val existing = DatasetWriter.fromJson(datasetFile.readText())
    val matches = WardPointJoin.join(existing, GeoJsonBoundaries.parse(wardFile.readText()), GeoJsonBoundaries.parse(districtFile.readText()))
    val updated = WardPointsApplier.apply(existing, matches, DatasetAttribution.WITH_WARD_POSITIONS, DatasetVersion.CURRENT)
        .let { it.copy(info = it.info.copy(generatedAt = options.generatedAt ?: existing.info.generatedAt)) }

    val violations = WardPointValidator.check(updated) + listOfNotNull(WardPointValidator.coverage(updated, options.minMatchRatio))
    val problem = violations.takeIf { it.isNotEmpty() }?.joinToString("\n") { "- ${it.kind}: ${it.message}" }

    options.outDir.mkdirs()
    File(options.outDir, "ward-points-report.md").writeText(WardPointsReport.report(updated, matches, options.minMatchRatio, problem))
    File(options.outDir, "ward-points-anomalies.csv").writeText(WardPointsReport.anomaliesCsv(updated, matches))
    val placed = matches.count { it.point != null }
    println("Wards with a position: $placed of ${matches.size}: ${if (problem == null) "PASSED" else "FAILED"}")
    violations.forEach { println("  ${it.kind}: ${it.message}") }
    if (problem != null) {
        println("tz-address.json was NOT written.")
        exitProcess(1)
    }
    DatasetWriter.write(datasetFile, updated)
    println("Output: ${options.outDir.path}")
}
