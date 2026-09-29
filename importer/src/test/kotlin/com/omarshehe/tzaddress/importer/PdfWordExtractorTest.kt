package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.importer.PdfFixtureBuilder.Placed
import java.io.File
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfWordExtractorTest {

    private fun pdf(pages: List<List<Placed>>, created: Calendar? = null): File =
        createTempFile("fixture", ".pdf").toFile().also { it.deleteOnExit(); PdfFixtureBuilder.build(it, pages, created) }

    @Test fun wordsKeepTheirDrawnPosition() {
        val page = PdfWordExtractor.extract(pdf(listOf(listOf(Placed(100.0, 150.0, "Kivukoni"))))).pages.single()
        val word = page.lines.single().words.single()
        assertEquals("Kivukoni", word.text)
        assertTrue(kotlin.math.abs(word.x0 - 100.0) < 0.5, "x0=${word.x0}")
    }

    @Test fun spaceSeparatedWords_areSplit() {
        val words = PdfWordExtractor.extract(pdf(listOf(listOf(Placed(100.0, 150.0, "Kariakoo Magharibi"))))).pages.single().lines.single().words
        assertEquals(listOf("Kariakoo", "Magharibi"), words.map { it.text })
    }

    @Test fun cellsInDifferentColumns_areDifferentWords() {
        val words = PdfWordExtractor.extract(pdf(listOf(listOf(Placed(100.0, 150.0, "Rugongwe"), Placed(300.0, 150.0, "47405"))))).pages.single().lines.single().words
        assertEquals(listOf("Rugongwe", "47405"), words.map { it.text })
    }

    @Test fun sameYCellsShareALine_andRowsAreOrderedTopDown() {
        val lines = PdfWordExtractor.extract(
            pdf(listOf(listOf(Placed(300.0, 166.0, "second"), Placed(100.0, 150.0, "a"), Placed(300.0, 150.0, "b")))),
        ).pages.single().lines
        assertEquals(listOf(listOf("a", "b"), listOf("second")), lines.map { l -> l.words.map { it.text } })
    }

    @Test fun blankPage_hasNoLines_andPageCountMatches() {
        val result = PdfWordExtractor.extract(pdf(listOf(listOf(Placed(100.0, 150.0, "x")), emptyList())))
        assertEquals(2, result.pages.size)
        assertEquals(emptyList(), result.pages[1].lines)
        assertEquals(listOf(1, 2), result.pages.map { it.page })
    }

    @Test fun creationDate_isReadAsIsoDate() {
        val created = GregorianCalendar(2012, Calendar.JULY, 30)
        assertEquals("2012-07-30", PdfWordExtractor.extract(pdf(listOf(listOf(Placed(1.0, 20.0, "x"))), created)).creationDate)
    }

    @Test fun missingCreationDate_isNull() {
        assertEquals(null, PdfWordExtractor.extract(pdf(listOf(listOf(Placed(1.0, 20.0, "x"))))).creationDate)
    }
}
