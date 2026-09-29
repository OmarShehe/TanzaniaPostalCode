package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.importer.TestPages.banner
import com.omarshehe.tzaddress.importer.TestPages.geometryOne
import com.omarshehe.tzaddress.importer.TestPages.geometryTwo
import com.omarshehe.tzaddress.importer.TestPages.headerA
import com.omarshehe.tzaddress.importer.TestPages.headerB
import com.omarshehe.tzaddress.importer.TestPages.headerZ
import com.omarshehe.tzaddress.importer.TestPages.line
import com.omarshehe.tzaddress.importer.TestPages.structure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LayoutDetectorTest {

    @Test
    fun emptyPage_isBlank() {
        assertEquals(PageKind.BLANK, LayoutDetector.detect(emptyList()).kind)
    }

    @Test
    fun banner_acceptsHyphenVariants() {
        listOf("‐", "-", "–", "‑").forEach { dash ->
            val s = structure(banner(90.0, "DAR ES SALAAM REGION $dash 11000"), headerB(120.0))
            assertEquals(Banner("DAR ES SALAAM", "11000"), s.banner, "dash U+%04X".format(dash.first().code))
        }
    }

    @Test
    fun layoutB_hasEightColumnsInReadingOrder() {
        val layout = assertNotNull(structure(headerB(120.0)).layout)
        assertEquals(LayoutKind.B, layout.kind)
        assertEquals(
            listOf(
                Role.REGION_NAME, Role.REGION_CODE, Role.DISTRICT_NAME, Role.DISTRICT_CODE,
                Role.WARD_NAME, Role.WARD_CODE, Role.MTAA, Role.KITONGOJI,
            ),
            layout.columns.map { it.role },
        )
        assertEquals(geometryOne, layout.columns.map { it.x })
    }

    @Test
    fun layoutA_firstColumnIsTheDistrict() {
        val layout = assertNotNull(structure(headerA(120.0)).layout)
        assertEquals(LayoutKind.A, layout.kind)
        assertEquals(
            listOf(Role.DISTRICT_NAME, Role.DISTRICT_CODE, Role.WARD_NAME, Role.WARD_CODE, Role.MTAA, Role.KITONGOJI),
            layout.columns.map { it.role },
        )
    }

    @Test
    fun layoutZ_endsWithShehiaAsMtaa() {
        val layout = assertNotNull(structure(headerZ(120.0)).layout)
        assertEquals(LayoutKind.Z, layout.kind)
        assertEquals(Role.MTAA, layout.columns.last().role)
        assertEquals(7, layout.columns.size)
    }

    @Test
    fun bodyExcludesBannerAndHeader() {
        val data = line(140.0, 77.0 to "MBEYA", 135.0 to "53")
        val s = structure(banner(90.0, "MBEYA REGION - 53000"), headerB(120.0), data)
        assertEquals(listOf(data), s.body)
    }

    @Test
    fun differentGeometries_mapSameCellsToSameRoles() {
        listOf(geometryOne, geometryTwo).forEach { xs ->
            val layout = assertNotNull(structure(headerB(120.0, xs)).layout)
            assertEquals(Role.MTAA, layout.roleAt(xs[6]))
            assertEquals(Role.KITONGOJI, layout.roleAt(xs[7] + 4.0))
            // postcode cells sit up to ~10pt right of their header
            assertEquals(Role.WARD_CODE, layout.roleAt(xs[5] + 10.0))
            assertEquals(Role.WARD_NAME, layout.roleAt(xs[4]))
        }
    }

    @Test
    fun cellFarLeftOfFirstColumn_hasNoRole() {
        val layout = assertNotNull(structure(headerB(120.0)).layout)
        assertNull(layout.roleAt(5.0))
    }

    @Test
    fun contentPageWithoutHeader_hasNoLayout() {
        val s = structure(line(100.0, 77.0 to "orphan"))
        assertEquals(PageKind.CONTENT, s.kind)
        assertNull(s.layout)
    }

    @Test
    fun secondBannerOnTheSamePage_startsALaterSegmentWithItsOwnHeaderAndRows() {
        val rowsA = line(140.0, 77.0 to "MBEYA")
        val rowsB = line(220.0, 77.0 to "DODOMA")
        val s = structure(
            banner(90.0, "MBEYA REGION - 53000"), headerB(110.0), rowsA,
            banner(180.0, "DODOMA REGION - 41000"), headerB(200.0, geometryTwo), rowsB,
        )
        assertEquals(Banner("MBEYA", "53000"), s.banner)
        assertEquals(listOf(rowsA), s.body)
        val later = s.later.single()
        assertEquals(Banner("DODOMA", "41000"), later.banner)
        assertEquals(geometryTwo, later.layout?.columns?.map { it.x })
        assertEquals(listOf(rowsB), later.body)
    }

    @Test
    fun secondHeaderMidPage_startsALaterSegmentWithoutABanner() {
        val s = structure(headerB(90.0), line(110.0, 77.0 to "a"), headerB(130.0, geometryTwo), line(150.0, 50.0 to "b"))
        val later = s.later.single()
        assertNull(later.banner)
        assertEquals(geometryTwo, later.layout?.columns?.map { it.x })
        assertEquals(1, later.body.size)
    }

    @Test
    fun singleBannerPage_hasNoLaterSegments() {
        assertEquals(emptyList(), structure(banner(90.0, "MBEYA REGION - 53000"), headerB(110.0)).later)
    }
}
