package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.importer.TestPages.banner
import com.omarshehe.tzaddress.importer.TestPages.headerB
import com.omarshehe.tzaddress.importer.TestPages.line
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditionImportTest {

    private val x = listOf(15.0, 70.0, 135.0, 190.0, 255.0, 300.0, 365.0, 430.0, 510.0)

    private fun pdf(file: File, vararg pages: List<Line>) =
        PdfFixtureBuilder.build(file, pages.map { it.flatMap(PdfFixtureBuilder::placed) })

    private fun regionFile(dir: File, name: String, districtCode: String, ward: String, old: String?) = pdf(
        File(dir, name),
        listOf(
            line(
                95.0, x[0] to "REGION", x[1] to "POSTCODE", x[2] to "DISTRICT", x[3] to "POSTCODE", x[4] to "WARD",
                x[5] to "POSTCODE", x[6] to "POSTCODE", x[7] to "MTAA/VILLAGE", x[8] to "KITONGOJI",
            ),
            line(
                120.0, x[1] to districtCode.take(2), x[2] to "ALPHA", x[3] to districtCode, x[4] to "Ward",
                x[5] to ward, x[6] to (old ?: ""), x[7] to "Mtaa A", x[8] to "Kitongoji A",
            ),
        ).map { it },
    )

    @Test fun mainlandFromRegionFiles_zanzibarFromThe2012List_andAChangesReport() {
        val dir = createTempDirectory("edition").toFile()
        val regions = File(dir, "regions").also { it.mkdirs() }
        regionFile(regions, "Songwe_54100.pdf", "541", "54101", "53814")
        regionFile(regions, "Arusha_23000.pdf", "231", "23101", null)
        val old = File(dir, "old.pdf")
        pdf(
            old,
            listOf(
                banner(72.0, "MJINI MAGHARIBI REGION - 71000"), headerB(100.0),
                line(120.0, 77.0 to "MJINI", 135.0 to "71", 195.0 to "MJINI", 272.0 to "711", 332.0 to "Ward Z", 416.0 to "71101", 478.0 to "Shehia Z", 585.0 to "K"),
            ),
        )
        val out = File(dir, "out").also { it.mkdirs() }
        File(out, "tz-address.json").writeText(
            DatasetWriter.toJson(DatasetDto(InfoDto("2", "2012", "t"), listOf(RegionDto("23000", "Arusha", listOf(DistrictDto("231", "District One", listOf(WardDto("23101", "Ward", emptyList())))))))),
        )
        val options = ImportOptions(
            pdf = old, pdfDir = regions, outDir = out, sourceEdition = null, generatedAt = "2026-01-01T00:00:00Z",
            policy = Policy(3, 0.5), force = false,
        )

        val log = ArrayList<String>()
        val code = PostcodeImport.run(options) { log += it }

        assertEquals(0, code, log.joinToString("\n"))
        val dataset = DatasetWriter.fromJson(File(out, "tz-address.json").readText())
        assertEquals(listOf("23000", "54100", "71000"), dataset.regions.map { it.code })
        assertEquals("Songwe", dataset.regions[1].name)
        assertTrue("2016-04-22" in dataset.info.sourceEdition && "Gazette Notice 240" in dataset.info.sourceEdition, dataset.info.sourceEdition)
        val changes = File(out, "edition-changes.md").readText()
        assertTrue("54100" in changes && "Songwe" in changes, changes)
        assertEquals(0, PostcodeImport.run(options) { })
        assertEquals(changes, File(out, "edition-changes.md").readText(), "a second run compares the edition with itself and must not overwrite the report")
        dir.deleteRecursively()
    }
}
