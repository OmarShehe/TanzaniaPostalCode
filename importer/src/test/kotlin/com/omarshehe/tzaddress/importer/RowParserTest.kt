package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.importer.TestPages.geometryOne
import com.omarshehe.tzaddress.importer.TestPages.headerB
import com.omarshehe.tzaddress.importer.TestPages.line
import com.omarshehe.tzaddress.importer.TestPages.structure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RowParserTest {

    private val layout = assertNotNull(structure(headerB(100.0)).layout)
    private val xs = geometryOne

    @Test
    fun multiWordCell_isJoinedWithSingleSpaces() {
        val row = RowParser.parse(line(120.0, xs[6] to "Kariakoo Magharibi"), layout)
        assertEquals("Kariakoo Magharibi", row.cells[Role.MTAA])
    }

    @Test
    fun adjacentCells_areNotMerged() {
        val row = RowParser.parse(line(120.0, xs[4] to "Rugongwe", xs[5] to "47405", xs[6] to "Kichananga", xs[7] to "Maendeleo"), layout)
        assertEquals("Rugongwe", row.cells[Role.WARD_NAME])
        assertEquals("47405", row.cells[Role.WARD_CODE])
        assertEquals("Kichananga", row.cells[Role.MTAA])
        assertEquals("Maendeleo", row.cells[Role.KITONGOJI])
    }

    @Test
    fun postcodeCellSlightlyRightOfHeader_landsInPostcodeColumn() {
        val row = RowParser.parse(line(120.0, xs[5] + 10.0 to "11101"), layout)
        assertEquals("11101", row.cells[Role.WARD_CODE])
    }

    @Test
    fun wordLeftOfFirstColumn_isUnassigned() {
        val row = RowParser.parse(line(120.0, 5.0 to "stray", xs[6] to "Kivukoni"), layout)
        assertEquals(listOf("stray"), row.unassigned.map { it.text })
        assertEquals("Kivukoni", row.cells[Role.MTAA])
    }

    @Test
    fun emptyColumns_areAbsentFromCells() {
        val row = RowParser.parse(line(120.0, xs[7] to "Mlimani"), layout)
        assertEquals(setOf(Role.KITONGOJI), row.cells.keys)
    }
}
