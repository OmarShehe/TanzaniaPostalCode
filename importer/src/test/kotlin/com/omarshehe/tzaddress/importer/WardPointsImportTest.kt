package com.omarshehe.tzaddress.importer

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** The `importWardPoints` run end to end over small files: success, the coverage gate, and usage errors. */
class WardPointsImportTest {
    private val dir = Files.createTempDirectory("ward-points").toFile()
    private val out = File(dir, "dataset")
    private val dataset = File(out, "tz-address.json")

    private fun square(name: String, lon: Double, lat: Double, size: Double = 1.0) = """
        {"type":"Feature","properties":{"shapeName":"$name"},"geometry":{"type":"Polygon","coordinates":[[[$lon,$lat],[${lon + size},$lat],[${lon + size},${lat + size}],[$lon,${lat + size}],[$lon,$lat]]]}}"""

    private fun collection(vararg features: String) = """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""

    private fun write(name: String, text: String) = File(dir, name).also { it.writeText(text) }

    private val wardFile = write(
        "wards.geojson",
        collection(square("Kati", 31.0, -4.0), square("Zeta", 32.0, -4.0), square("Eta", 33.0, -4.0), """{"type":"Feature","properties":{"shapeName":"Empty"},"geometry":null}"""),
    )
    private val districtFile = write("districts.geojson", collection(square("Alpha", 30.0, -5.0, 5.0)))

    init {
        out.mkdirs()
        DatasetWriter.write(
            dataset,
            DatasetDto(
                InfoDto("1", "2012-07-30", "2026-09-29T00:00:00Z"),
                listOf(
                    RegionDto(
                        "10000", "R",
                        listOf(DistrictDto("11", "Alpha", listOf("Kati", "Zeta", "Eta", "Missing").mapIndexed { i, n -> DatasetFixtures.ward("1110${i + 1}", n) })),
                    ),
                ),
            ),
        )
    }

    private fun options(ratio: String) = WardPointsOptions.parse(
        arrayOf("--wardBoundaries=${wardFile.path}", "--districtBoundaries=${districtFile.path}", "--outDir=${out.path}", "--minMatchRatio=$ratio"),
    )

    private fun wards() = DatasetWriter.fromJson(dataset.readText()).regions.single().districts.single().wards.associateBy { it.postcode }

    @Test
    fun successWritesPositionsAttributionVersionAndReports() {
        assertEquals(0, WardPointsImport.run(options("0.7")) {})
        val back = DatasetWriter.fromJson(dataset.readText())
        assertEquals(-3.5, wards().getValue("11101").latitude)
        assertEquals(31.5, wards().getValue("11101").longitude)
        assertNull(wards().getValue("11104").latitude)
        assertContains(back.info.attribution, "OpenStreetMap")
        assertEquals(DatasetVersion.CURRENT, back.info.version)
        assertEquals("2026-09-29T00:00:00Z", back.info.generatedAt)
        val report = File(out, "ward-points-report.md").readText()
        assertContains(report, "PASSED")
        assertContains(report, "Boundary features skipped (no name or no usable geometry): 1")
        assertContains(File(out, "ward-points-anomalies.csv").readText(), "11104,R,Alpha,Missing,NO_MATCH")
    }

    @Test
    fun belowTheCoverageGateTheDatasetIsLeftUntouchedAndTheReportSaysFailed() {
        val before = dataset.readBytes()
        assertEquals(1, WardPointsImport.run(options("0.9")) {})
        assertEquals(before.toList(), dataset.readBytes().toList())
        assertContains(File(out, "ward-points-report.md").readText(), "FAILED")
        assertNotNull(File(out, "ward-points-anomalies.csv").takeIf { it.isFile })
    }

    @Test
    fun missingBoundaryFilesOrDatasetAreUsageErrors() {
        assertEquals(2, WardPointsImport.run(WardPointsOptions.parse(arrayOf("--outDir=${out.path}"))) {})
        dataset.delete()
        assertEquals(2, WardPointsImport.run(options("0.7")) {})
    }
}
