package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.importer.PdfFixtureBuilder.Placed
import com.omarshehe.tzaddress.importer.TestPages.banner
import com.omarshehe.tzaddress.importer.TestPages.geometryOne
import com.omarshehe.tzaddress.importer.TestPages.headerB
import com.omarshehe.tzaddress.importer.TestPages.line
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The `importPostcodes` run end to end: a re-import of the list must not lose ward positions written by `importWardPoints`. */
class PostcodeImportTest {
    private val dir = Files.createTempDirectory("postcode-import").toFile()
    private val pdf = File(dir, "list.pdf").also { file ->
        val a = geometryOne
        val page = listOf(
            banner(72.0, "MBEYA REGION - 53000"), headerB(100.0, a),
            line(120.0, a[0] to "MBEYA", a[1] to "53", a[2] to "MBEYA CBD", a[3] to "531", a[4] to "Ward One", a[5] to "53101", a[6] to "Mtaa One", a[7] to "K1"),
            line(140.0, a[4] to "Ward Two", a[5] to "53102", a[6] to "Mtaa Two"),
        )
        PdfFixtureBuilder.build(file, listOf(page, emptyList()).map { lines -> lines.flatMap(PdfFixtureBuilder::placed) })
    }
    private val out = File(dir, "dataset")
    private val dataset = File(out, "tz-address.json")
    private val args = arrayOf("--pdf=${pdf.path}", "--outDir=${out.path}", "--expectedRegions=1", "--sourceEdition=test", "--generatedAt=2026-01-01T00:00:00Z")

    private fun wards() = DatasetWriter.fromJson(dataset.readText()).regions.single().districts.single().wards.associateBy { it.postcode }

    @Test
    fun reimportKeepsPositionsAndAttributionOfAnEarlierWardPointsImport() {
        out.mkdirs()
        DatasetWriter.write(
            dataset,
            DatasetDto(
                InfoDto("2", "test", "2025-01-01T00:00:00Z", "© OpenStreetMap contributors"),
                listOf(RegionDto("53000", "Mbeya", listOf(DistrictDto("531", "Mbeya Cbd", listOf(WardDto("53101", "Ward One", emptyList(), -8.9, 33.4)))))),
            ),
        )
        assertEquals(0, PostcodeImport.run(ImportOptions.parse(args)) {})
        val back = DatasetWriter.fromJson(dataset.readText())
        assertEquals(-8.9, wards().getValue("53101").latitude)
        assertEquals(33.4, wards().getValue("53101").longitude)
        assertNull(wards().getValue("53102").latitude)
        assertEquals("© OpenStreetMap contributors", back.info.attribution)
        assertEquals("2026-01-01T00:00:00Z", back.info.generatedAt)
    }

    @Test
    fun firstImportHasNoPositionsAndNoAttribution() {
        assertEquals(0, PostcodeImport.run(ImportOptions.parse(args)) {})
        assertNull(wards().getValue("53101").latitude)
        assertEquals("", DatasetWriter.fromJson(dataset.readText()).info.attribution)
    }

    @Test
    fun aMissingPdfIsAUsageError() {
        val messages = ArrayList<String>()
        assertEquals(2, PostcodeImport.run(ImportOptions.parse(arrayOf("--outDir=${out.path}")), messages::add))
        assertEquals(true, messages.any { "Missing -Ppdf" in it })
    }
}
