package com.omarshehe.tzaddress.importer

import java.io.File

/** The `importWardPoints` run: adds a position to each ward of the committed dataset from boundary GeoJSON. */
object WardPointsImport {

    /** Returns the process exit code: 0 passed, 1 failed a check (dataset not written), 2 usage error. */
    fun run(options: WardPointsOptions, log: (String) -> Unit = ::println): Int {
        val wardFile = options.wardBoundaries
        val districtFile = options.districtBoundaries
        if (wardFile == null || districtFile == null || !wardFile.isFile || !districtFile.isFile) {
            log("Usage: ./gradlew :importer:importWardPoints -PwardBoundaries=/path/ward.geojson -PdistrictBoundaries=/path/district.geojson [-PminMatchRatio=0.75] [-PgeneratedAt=..] [-PoutDir=dataset]")
            log("Both -PwardBoundaries and -PdistrictBoundaries must point to existing files.")
            return 2
        }
        val datasetFile = File(options.outDir, "tz-address.json")
        if (!datasetFile.isFile) {
            log("${datasetFile.path} not found; run :importer:importPostcodes first.")
            return 2
        }

        val existing = DatasetWriter.fromJson(datasetFile.readText())
        val wards = GeoJsonBoundaries.read(wardFile.readText())
        val districts = GeoJsonBoundaries.read(districtFile.readText())
        val matches = WardPointJoin.join(existing, wards.features, districts.features)
        val updated = WardPointsApplier.apply(existing, matches, DatasetAttribution.WITH_WARD_POSITIONS, DatasetVersion.CURRENT)
            .let { it.copy(info = it.info.copy(generatedAt = options.generatedAt ?: existing.info.generatedAt)) }

        val violations = WardPointValidator.check(updated) + listOfNotNull(WardPointValidator.coverage(updated, options.minMatchRatio))
        val problem = violations.takeIf { it.isNotEmpty() }?.joinToString("\n") { "- ${it.kind}: ${it.message}" }

        options.outDir.mkdirs()
        val skipped = wards.skipped
        File(options.outDir, "ward-points-report.md").writeText(WardPointsReport.report(updated, matches, options.minMatchRatio, problem, skipped))
        File(options.outDir, "ward-points-anomalies.csv").writeText(WardPointsReport.anomaliesCsv(updated, matches))
        log("Wards with a position: ${matches.count { it.point != null }} of ${matches.size}: ${if (problem == null) "PASSED" else "FAILED"}")
        if (skipped > 0) log("Boundary features skipped (no name or no usable geometry): $skipped")
        violations.forEach { log("  ${it.kind}: ${it.message}") }
        if (problem != null) {
            log("tz-address.json was NOT written.")
            return 1
        }
        DatasetWriter.write(datasetFile, updated)
        log("Output: ${options.outDir.path}")
        return 0
    }
}
