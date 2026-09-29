package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.importer.PdfFixtureBuilder.Placed
import com.omarshehe.tzaddress.importer.TestPages.banner
import com.omarshehe.tzaddress.importer.TestPages.geometryOne
import com.omarshehe.tzaddress.importer.TestPages.geometryTwo
import com.omarshehe.tzaddress.importer.TestPages.headerB
import com.omarshehe.tzaddress.importer.TestPages.line
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfImportEndToEndTest {

    @Test fun twoPagesWithDifferentColumnGeometry_parseIntoOneRegionTree() {
        val a = geometryOne
        val b = geometryTwo
        val page1 = listOf(
            banner(72.0, "MBEYA REGION - 53000"), headerB(100.0, a),
            line(120.0, a[0] to "MBEYA", a[1] to "53", a[2] to "MBEYA CBD", a[3] to "531", a[4] to "Ward One", a[5] to "53101", a[6] to "Mtaa One", a[7] to "K1"),
        )
        val page2 = listOf(headerB(72.0, b), line(92.0, b[7] to "K2"), line(108.0, b[4] to "Ward Two", b[5] to "53102", b[6] to "Mtaa Two"))
        val file = createTempFile("e2e", ".pdf").toFile().also { it.deleteOnExit() }
        PdfFixtureBuilder.build(file, listOf(page1, page2, emptyList()).map { lines -> lines.flatMap(PdfFixtureBuilder::placed) })

        val result = ImportPipeline.parse(PdfWordExtractor.extract(file))

        val wards = result.regions.single().districts.single().wards
        assertEquals(listOf("53101", "53102"), wards.map { it.postcode })
        assertEquals(listOf("K1", "K2"), wards[0].mtaas.single().kitongojis)
        assertEquals("Mtaa Two", wards[1].mtaas.single().name)
        assertTrue(result.anomalies.isEmpty(), result.anomalies.toString())
    }
}
