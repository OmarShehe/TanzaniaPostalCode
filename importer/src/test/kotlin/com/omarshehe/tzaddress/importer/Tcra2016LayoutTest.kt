package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.importer.TestPages.headerA
import com.omarshehe.tzaddress.importer.TestPages.headerB
import com.omarshehe.tzaddress.importer.TestPages.line
import com.omarshehe.tzaddress.importer.TestPages.structure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Tcra2016LayoutTest {

    private val x = listOf(50.0, 100.0, 150.0, 210.0, 260.0, 330.0, 390.0, 450.0, 560.0)
    private val region = Banner("Songwe", "54100")

    private val rolesWithOld = listOf(
        Role.REGION_NAME, Role.REGION_CODE, Role.DISTRICT_NAME, Role.DISTRICT_CODE,
        Role.WARD_NAME, Role.WARD_CODE, Role.OLD_WARD_CODE, Role.MTAA, Role.KITONGOJI,
    )

    private fun build(vararg pages: PageStructure): BuildResult {
        val builder = HierarchyBuilder()
        builder.startRegion(region)
        pages.forEachIndexed { i, p -> builder.addPage(i + 1, p) }
        return builder.result()
    }

    @Test
    fun oldAboveTheHeader_addsAnOldPostcodeColumnAtTheOldWord() {
        val s = structure(line(90.0, 392.0 to "OLD"), headerB(110.0, listOf(50.0, 100.0, 150.0, 210.0, 260.0, 330.0, 450.0, 560.0)), line(125.0, 390.0 to "POSTCODE"))
        val layout = assertNotNull(s.layout)
        assertEquals(rolesWithOld, layout.columns.map { it.role })
        assertEquals(390.0, layout.columns.first { it.role == Role.OLD_WARD_CODE }.x)
        assertTrue(s.body.isEmpty(), "the stacked OLD and POSTCODE lines are header text, not data: ${s.body}")
    }

    @Test
    fun oldOnTheHeaderLine_isTheOldPostcodeColumn() {
        val header = line(
            110.0, x[0] to "REGION", x[1] to "POSTCODE", x[2] to "DISTRICT", x[3] to "POSTCODE", x[4] to "WARD",
            x[5] to "POSTCODE", x[6] to "OLD", x[7] to "MTAA/VILLAGE", x[8] to "KITONGOJI",
        )
        val layout = assertNotNull(structure(header, line(125.0, x[6] to "POSTCODE")).layout)
        assertEquals(rolesWithOld, layout.columns.map { it.role })
    }

    @Test
    fun aSecondPostcodeAfterTheWardCode_isTheOldPostcodeColumn() {
        val header = line(
            110.0, x[0] to "REGION", x[1] to "POSTCODE", x[2] to "DISTRICT", x[3] to "POSTCODE", x[4] to "WARD",
            x[5] to "POSTCODE", x[6] to "POSTCODE", x[7] to "MTAA/VILLAGE", x[8] to "KITONGOJI",
        )
        val s = structure(line(95.0, x[6] to "OLD"), header)
        assertEquals(rolesWithOld, assertNotNull(s.layout).columns.map { it.role })
        assertTrue(s.body.isEmpty())
    }

    @Test
    fun withoutAnOldColumn_theLayoutIsUnchanged() {
        val layout = assertNotNull(structure(headerB(110.0)).layout)
        assertTrue(Role.OLD_WARD_CODE !in layout.columns.map { it.role })
    }

    @Test
    fun dalEsSalaamShape_districtFirstWithAnOldColumn() {
        val header = line(
            110.0, x[0] to "REGION", x[1] to "POSTCODE", x[4] to "WARD", x[5] to "POSTCODE", x[6] to "POSTCODE",
            x[7] to "MTAA/VILLAGE", x[8] to "KITONGOJI",
        )
        val layout = assertNotNull(structure(line(95.0, x[6] to "OLD"), header).layout)
        assertEquals(
            listOf(Role.DISTRICT_NAME, Role.DISTRICT_CODE, Role.WARD_NAME, Role.WARD_CODE, Role.OLD_WARD_CODE, Role.MTAA, Role.KITONGOJI),
            layout.columns.map { it.role },
        )
    }

    @Test
    fun aRegionStartedByTheCaller_hasNoBannerOnItsPages() {
        val r = build(
            structure(
                line(95.0, x[6] to "OLD"),
                line(
                    110.0, x[0] to "REGION", x[1] to "POSTCODE", x[2] to "DISTRICT", x[3] to "POSTCODE", x[4] to "WARD",
                    x[5] to "POSTCODE", x[6] to "POSTCODE", x[7] to "MTAA/VILLAGE", x[8] to "KITONGOJI",
                ),
                line(130.0, x[1] to "54", x[2] to "SONGWE", x[3] to "541", x[4] to "Gua", x[5] to "54101", x[6] to "53814", x[7] to "Gua", x[8] to "Gua A"),
                line(146.0, x[4] to "Some", x[5] to "54102", x[7] to "Some"),
            ),
        )
        val reg = r.regions.single()
        assertEquals("54100", reg.code)
        assertEquals("Songwe", reg.name)
        val wards = reg.districts.single().wards
        assertEquals(listOf("54101", "54102"), wards.map { it.postcode })
        assertEquals("53814", wards[0].oldPostcode)
        assertEquals(null, wards[1].oldPostcode)
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun theHeaderOnlyOnTheFirstPage_isReusedOnLaterPages() {
        val first = structure(
            line(95.0, x[6] to "OLD"),
            line(
                110.0, x[0] to "REGION", x[1] to "POSTCODE", x[2] to "DISTRICT", x[3] to "POSTCODE", x[4] to "WARD",
                x[5] to "POSTCODE", x[6] to "POSTCODE", x[7] to "MTAA/VILLAGE", x[8] to "KITONGOJI",
            ),
            line(130.0, x[1] to "54", x[2] to "SONGWE", x[3] to "541", x[4] to "Gua", x[5] to "54101", x[7] to "Gua"),
        )
        val second = structure(line(80.0, x[4] to "Some", x[5] to "54102", x[6] to "54999", x[7] to "Some"))
        val wards = build(first, second).regions.single().districts.single().wards
        assertEquals(listOf("54101", "54102"), wards.map { it.postcode })
        assertEquals("54999", wards[1].oldPostcode)
    }

    @Test
    fun oldPostcodeThatIsNotFiveDigits_isFlaggedAndNotKept() {
        val r = build(
            structure(
                line(95.0, x[6] to "OLD"),
                line(
                    110.0, x[0] to "REGION", x[1] to "POSTCODE", x[2] to "DISTRICT", x[3] to "POSTCODE", x[4] to "WARD",
                    x[5] to "POSTCODE", x[6] to "POSTCODE", x[7] to "MTAA/VILLAGE", x[8] to "KITONGOJI",
                ),
                line(130.0, x[1] to "54", x[2] to "SONGWE", x[3] to "541", x[4] to "Gua", x[5] to "54101", x[6] to "5381", x[7] to "Gua"),
            ),
        )
        assertEquals(null, r.regions.single().districts.single().wards.single().oldPostcode)
        assertEquals(listOf(AnomalyKind.UNEXPECTED_CELL), r.anomalies.map { it.kind })
    }

    @Test
    fun layoutAHeaderStillWorks_whenNoOldWordIsPresent() {
        assertEquals(LayoutKind.A, assertNotNull(structure(headerA(110.0)).layout).kind)
    }

    @Test
    fun stackedPostcodeBelowTheHeader_movesTheOldColumnLeftToWhereTheCellsStart() {
        val header = line(
            110.0, x[0] to "REGION", x[1] to "POSTCODE", x[2] to "DISTRICT", x[3] to "POSTCODE", x[4] to "WARD",
            x[5] to "POSTCODE", x[6] + 20.0 to "OLD", x[7] to "MTAA/VILLAGE", x[8] to "KITONGOJI",
        )
        val s = structure(header, line(125.0, x[6] to "POSTCODE"))
        assertEquals(x[6], assertNotNull(s.layout).columns.first { it.role == Role.OLD_WARD_CODE }.x)
    }

    @Test
    fun headerOncePerRegion_laterPagesWithoutAHeaderAreNotAnomalies() {
        val builder = HierarchyBuilder(headerOncePerRegion = true)
        builder.startRegion(region)
        builder.addPage(
            1,
            structure(
                line(
                    110.0, x[0] to "REGION", x[1] to "POSTCODE", x[2] to "DISTRICT", x[3] to "POSTCODE", x[4] to "WARD",
                    x[5] to "POSTCODE", x[6] to "POSTCODE", x[7] to "MTAA/VILLAGE", x[8] to "KITONGOJI",
                ),
                line(130.0, x[1] to "54", x[2] to "SONGWE", x[3] to "541", x[4] to "Gua", x[5] to "54101", x[7] to "Gua"),
            ),
        )
        builder.addPage(2, structure(line(80.0, x[4] to "Some", x[5] to "54102", x[7] to "Some")))
        val r = builder.result()
        assertEquals(2, r.regions.single().districts.single().wards.size)
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun aBannerRepeatingTheRegionAlreadyStarted_doesNotStartASecondRegion() {
        val builder = HierarchyBuilder(headerOncePerRegion = true)
        builder.startRegion(region)
        builder.addPage(
            1,
            structure(
                TestPages.banner(90.0, "SONGWE REGION - 54100"),
                line(
                    110.0, x[0] to "REGION", x[1] to "POSTCODE", x[2] to "DISTRICT", x[3] to "POSTCODE", x[4] to "WARD",
                    x[5] to "POSTCODE", x[6] to "POSTCODE", x[7] to "MTAA/VILLAGE", x[8] to "KITONGOJI",
                ),
                line(130.0, x[1] to "54", x[2] to "SONGWE", x[3] to "541", x[4] to "Gua", x[5] to "54101", x[7] to "Gua"),
            ),
        )
        val r = builder.result()
        assertEquals(1, r.regions.size)
        assertEquals(1, r.regions.single().districts.single().wards.size)
    }

    @Test
    fun aMtaaPrintedInTheDistrictColumn_staysAMtaaOfTheCurrentWard() {
        val builder = HierarchyBuilder(headerOncePerRegion = true)
        builder.startRegion(Banner("Tabora", "45000"))
        builder.addPage(
            1,
            structure(
                line(
                    110.0, x[0] to "REGION", x[1] to "POSTCODE", x[2] to "DISTRICT", x[3] to "POSTCODE", x[4] to "WARD",
                    x[5] to "POSTCODE", x[7] to "MTAA/VILLAGE", x[8] to "KITONGOJI",
                ),
                line(130.0, x[1] to "45", x[2] to "TABORA CBD", x[3] to "451", x[4] to "Ngambo", x[5] to "45101", x[7] to "Tukutuku"),
                line(146.0, x[2] to "Sokoni", x[7] to "Kazehil"),
                line(162.0, x[7] to "Kiyungi"),
            ),
        )
        val r = builder.result()
        val district = r.regions.single().districts.single()
        assertEquals("TABORA CBD".lowercase(), district.name.lowercase())
        assertEquals(listOf("Tukutuku", "Sokoni", "Kazehil", "Kiyungi"), district.wards.single().mtaas.map { it.name })
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }
}
