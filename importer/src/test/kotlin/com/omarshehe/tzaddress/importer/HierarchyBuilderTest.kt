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
import kotlin.test.assertTrue

class HierarchyBuilderTest {

    private val x = geometryOne

    private fun build(vararg pages: PageStructure): BuildResult {
        val builder = HierarchyBuilder()
        pages.forEachIndexed { i, p -> builder.addPage(i + 1, p) }
        return builder.result()
    }

    @Test
    fun layoutB_buildsRegionDistrictWardMtaaKitongoji() {
        val r = build(
            structure(
                banner(90.0, "MBEYA REGION - 53000"), headerB(110.0),
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531", x[4] to "Mbalizi Road", x[5] to "53101", x[6] to "Sabasaba", x[7] to "Mlimani"),
                line(146.0, x[7] to "Mwanjelwa"),
                line(162.0, x[6] to "Isanga", x[7] to "Kati"),
            ),
        )
        val region = r.regions.single()
        assertEquals("53000", region.code)
        assertEquals("Mbeya", region.name)
        val district = region.districts.single()
        assertEquals("531", district.code)
        assertEquals("Mbeya CBD", district.name)
        val ward = district.wards.single()
        assertEquals("53101", ward.postcode)
        assertEquals("Mbalizi Road", ward.name)
        assertEquals(listOf("Sabasaba", "Isanga"), ward.mtaas.map { it.name })
        assertEquals(listOf("Mlimani", "Mwanjelwa"), ward.mtaas[0].kitongojis)
        assertEquals(listOf("Kati"), ward.mtaas[1].kitongojis)
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun layoutA_firstColumnIsDistrictWithTwoDigitCode() {
        val r = build(
            structure(
                banner(90.0, "DAR ES SALAAM REGION - 11000"), headerA(110.0),
                line(130.0, x[0] to "ILALA CBD", x[1] to "11", x[4] to "KIVUKONI", x[5] to "11101", x[6] to "Kivukoni"),
                line(146.0, x[6] to "Sea View"),
            ),
        )
        val district = r.regions.single().districts.single()
        assertEquals("11", district.code)
        assertEquals("Ilala CBD", district.name)
        assertEquals("Kivukoni", district.wards.single().name)
        assertEquals(listOf("Kivukoni", "Sea View"), district.wards.single().mtaas.map { it.name })
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun layoutZ_shehiaBecomeMtaasWithoutKitongojis() {
        val r = build(
            structure(
                banner(90.0, "MJINI MAGHARIBI REGION - 71000"), headerZ(110.0),
                line(130.0, x[0] to "MJINI MAGHARIBI", x[1] to "71", x[2] to "MJINI", x[3] to "711", x[4] to "Mkunazini", x[5] to "71101", x[6] to "Mkunazini"),
                line(146.0, x[6] to "Kiponda"),
            ),
        )
        val ward = r.regions.single().districts.single().wards.single()
        assertEquals(listOf("Mkunazini", "Kiponda"), ward.mtaas.map { it.name })
        assertTrue(ward.mtaas.all { it.kitongojis.isEmpty() })
    }

    @Test
    fun pageBreak_kitongojiRowsAfterRepeatedHeaderContinueTheSameMtaa() {
        val first = structure(
            banner(90.0, "MBEYA REGION - 53000"), headerB(110.0),
            line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531", x[4] to "Ward One", x[5] to "53101", x[6] to "Mtaa One", x[7] to "K1"),
        )
        val second = structure(headerB(90.0, geometryTwo), line(110.0, geometryTwo[7] to "K2"), line(126.0, geometryTwo[7] to "K3"))
        val mtaa = build(first, second).regions.single().districts.single().wards.single().mtaas.single()
        assertEquals(listOf("K1", "K2", "K3"), mtaa.kitongojis)
    }

    @Test
    fun blankLeftColumns_belongToTheCurrentWard() {
        val r = build(
            structure(
                banner(90.0, "MBEYA REGION - 53000"), headerB(110.0),
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531", x[4] to "Ward One", x[5] to "53101", x[6] to "Mtaa One"),
                line(146.0, x[6] to "Mtaa Two"),
                line(162.0, x[4] to "Ward Two", x[5] to "53102", x[6] to "Mtaa Three"),
            ),
        )
        val wards = r.regions.single().districts.single().wards
        assertEquals(listOf("Mtaa One", "Mtaa Two"), wards[0].mtaas.map { it.name })
        assertEquals(listOf("Mtaa Three"), wards[1].mtaas.map { it.name })
    }

    @Test
    fun duplicateMtaaNames_arePreservedInOrder() {
        val r = build(
            structure(
                banner(90.0, "MBEYA REGION - 53000"), headerB(110.0),
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531", x[4] to "Ward One", x[5] to "53101", x[6] to "Rubumba"),
                line(146.0, x[6] to "Rubumba"),
            ),
        )
        assertEquals(listOf("Rubumba", "Rubumba"), r.regions.single().districts.single().wards.single().mtaas.map { it.name })
    }

    @Test
    fun namesWithQuotesAndApostrophes_survive() {
        val r = build(
            structure(
                banner(90.0, "MBEYA REGION - 53000"), headerB(110.0),
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531", x[4] to "Jang'ombe", x[5] to "53101", x[6] to "Mtambani \"A\""),
            ),
        )
        val ward = r.regions.single().districts.single().wards.single()
        assertEquals("Jang'ombe", ward.name)
        assertEquals("Mtambani \"A\"", ward.mtaas.single().name)
    }

    @Test
    fun kitongojiWithoutMtaa_isKeptUnderImplicitMtaaAndFlagged() {
        val r = build(
            structure(
                banner(90.0, "MBEYA REGION - 53000"), headerB(110.0),
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBOZI", x[3] to "533", x[4] to "Vwawa", x[5] to "53301", x[7] to "Isangu"),
            ),
        )
        val ward = r.regions.single().districts.single().wards.single()
        assertEquals("Vwawa", ward.mtaas.single().name)
        assertEquals(listOf("Isangu"), ward.mtaas.single().kitongojis)
        assertEquals(listOf(AnomalyKind.KITONGOJI_WITHOUT_MTAA), r.anomalies.map { it.kind })
    }

    @Test
    fun badWardPostcode_isFlaggedButWardIsRecorded() {
        val r = build(
            structure(
                banner(90.0, "MBEYA REGION - 53000"), headerB(110.0),
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531", x[4] to "Ward One", x[5] to "5310", x[6] to "Mtaa One"),
            ),
        )
        assertEquals(1, r.regions.single().districts.single().wards.size)
        assertEquals(listOf(AnomalyKind.BAD_WARD_POSTCODE), r.anomalies.map { it.kind })
    }

    @Test
    fun mtaaBeforeAnyWard_isFlaggedNotFatal() {
        val r = build(structure(banner(90.0, "MBEYA REGION - 53000"), headerB(110.0), line(130.0, x[6] to "Lost")))
        assertEquals(listOf(AnomalyKind.ORPHAN_ROW), r.anomalies.map { it.kind })
        assertEquals(1, r.anomalies.single().page)
    }

    @Test
    fun secondBanner_startsANewRegion() {
        val r = build(
            structure(
                banner(90.0, "ARUSHA REGION - 23000"), headerB(110.0),
                line(130.0, x[0] to "ARUSHA", x[1] to "23", x[2] to "MERU", x[3] to "231", x[4] to "W", x[5] to "23101", x[6] to "M"),
            ),
            structure(
                banner(90.0, "DODOMA REGION - 41000"), headerB(110.0),
                line(130.0, x[0] to "DODOMA", x[1] to "41", x[2] to "CHAMWINO", x[3] to "411", x[4] to "W2", x[5] to "41101", x[6] to "M2"),
            ),
        )
        assertEquals(listOf("23000", "41000"), r.regions.map { it.code })
        assertEquals(listOf("Arusha", "Dodoma"), r.regions.map { it.name })
    }

    @Test
    fun contentPageWithoutHeader_reusesPreviousLayoutAndFlagsIt() {
        val r = build(
            structure(
                banner(90.0, "MBEYA REGION - 53000"), headerB(110.0),
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531", x[4] to "Ward One", x[5] to "53101", x[6] to "Mtaa One"),
            ),
            structure(line(90.0, x[6] to "Mtaa Two")),
        )
        assertEquals(listOf("Mtaa One", "Mtaa Two"), r.regions.single().districts.single().wards.single().mtaas.map { it.name })
        assertEquals(listOf(AnomalyKind.MISSING_HEADER), r.anomalies.map { it.kind })
    }

    @Test
    fun blankPages_areIgnored() {
        val r = build(structure())
        assertTrue(r.regions.isEmpty())
        assertTrue(r.anomalies.isEmpty())
    }

    @Test
    fun dataLineCount_countsBodyLines() {
        val r = build(
            structure(
                banner(90.0, "MBEYA REGION - 53000"), headerB(110.0),
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531", x[4] to "Ward One", x[5] to "53101", x[6] to "Mtaa One"),
                line(146.0, x[6] to "Mtaa Two"),
            ),
        )
        assertEquals(2, r.dataLineCount)
    }

    private val bHead = arrayOf(
        banner(90.0, "MBEYA REGION - 53000"), headerB(110.0),
    )

    private fun rowWithDistrict(y: Double) =
        line(y, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531")

    @Test
    fun wardCodeOnTheNextLine_isPairedWithTheWardAbove() {
        val r = build(
            structure(
                *bHead,
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531", x[4] to "Olorien", x[6] to "Olorien"),
                line(146.0, x[5] to "53210"),
            ),
        )
        val ward = r.regions.single().districts.single().wards.single()
        assertEquals("53210", ward.postcode)
        assertEquals("Olorien", ward.name)
        assertEquals(listOf("Olorien"), ward.mtaas.map { it.name })
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun wrappedWardName_isMergedWithItsSecondLine() {
        val r = build(
            structure(
                *bHead,
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531", x[4] to "Ward One", x[5] to "53101", x[6] to "Mtaa One"),
                line(146.0, x[4] to "Uwanja wa"),
                line(162.0, x[4] to "taifa", x[5] to "53105", x[6] to "Kidugalo"),
            ),
        )
        val wards = r.regions.single().districts.single().wards
        assertEquals(listOf("53101", "53105"), wards.map { it.postcode })
        assertEquals("Uwanja wa Taifa", wards[1].name)
        assertEquals(listOf("Kidugalo"), wards[1].mtaas.map { it.name })
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun districtCodeOnTheNextLine_isPairedWithTheDistrictAbove() {
        val r = build(
            structure(
                *bHead,
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "BABATI"),
                line(146.0, x[3] to "531", x[4] to "Magugu", x[5] to "53101", x[6] to "Magugu"),
            ),
        )
        val district = r.regions.single().districts.single()
        assertEquals("531", district.code)
        assertEquals("Magugu", district.wards.single().name)
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun wardWithoutAnyCode_isFlaggedOnceWhenNoCodeFollows() {
        val r = build(
            structure(
                *bHead,
                rowWithDistrict(130.0),
                line(146.0, x[4] to "Nocode", x[6] to "M1"),
                line(162.0, x[6] to "M2"),
            ),
        )
        assertEquals(listOf(AnomalyKind.BAD_WARD_POSTCODE), r.anomalies.map { it.kind })
        assertEquals(2, r.anomalies.single().line)
    }

    @Test
    fun districtWithoutAnyCode_isFlaggedWhenNoCodeFollows() {
        val r = build(
            structure(
                *bHead,
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "LOST"),
                line(146.0, x[4] to "W", x[5] to "53101", x[6] to "M"),
            ),
        )
        assertEquals(listOf(AnomalyKind.DISTRICT_WITHOUT_CODE), r.anomalies.map { it.kind })
    }

    @Test
    fun pendingWard_isFlaggedAtTheEndOfTheInput() {
        val r = build(structure(*bHead, rowWithDistrict(130.0), line(146.0, x[4] to "Nocode")))
        assertEquals(listOf(AnomalyKind.BAD_WARD_POSTCODE), r.anomalies.map { it.kind })
    }

    @Test
    fun kitongojiWithoutMtaa_isInformationalAndExcludedFromSuspectCount() {
        val r = build(
            structure(
                *bHead,
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBOZI", x[3] to "533", x[4] to "Vwawa", x[5] to "53301", x[7] to "Isangu"),
                line(146.0, x[4] to "Bad", x[5] to "5330", x[6] to "M"),
            ),
        )
        assertEquals(2, r.anomalies.size)
        assertEquals(1, r.suspectAnomalyCount)
    }

    @Test
    fun wardNameWrappedBelow_isAppendedToThePreviousWard() {
        val r = build(
            structure(
                *bHead,
                rowWithDistrict(130.0),
                line(146.0, x[4] to "Oloirien/", x[5] to "23702", x[6] to "Oloirien/ Magaiduru"),
                line(162.0, x[4] to "Magaiduru"),
                line(178.0, x[6] to "Oldonyowas"),
            ),
        )
        val wards = r.regions.single().districts.single().wards
        assertEquals(1, wards.size)
        assertEquals("Oloirien/ Magaiduru", wards.single().name)
        assertEquals("23702", wards.single().postcode)
        assertEquals(listOf("Oloirien/ Magaiduru", "Oldonyowas"), wards.single().mtaas.map { it.name })
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun districtNameWrappedAbove_isMergedWithItsSecondLine() {
        val r = build(
            structure(
                *bHead,
                line(130.0, x[2] to "KIGOMA"),
                line(146.0, x[0] to "KIGOMA", x[1] to "47", x[2] to "CBD", x[3] to "471", x[4] to "Kigoma", x[5] to "47101", x[6] to "Katonga"),
            ),
        )
        val district = r.regions.single().districts.single()
        assertEquals("Kigoma CBD", district.name)
        assertEquals("471", district.code)
        assertEquals("Kigoma", district.wards.single().name)
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun districtNameWrappedBelow_isAppendedAndItsWardsAreMoved() {
        val r = build(
            structure(
                *bHead,
                line(130.0, x[0] to "RUKWA", x[1] to "55", x[2] to "SUMBAWANGA", x[3] to "551"),
                line(146.0, x[2] to "CBD", x[4] to "Sumbawanga", x[5] to "55101", x[6] to "Katuma"),
                line(162.0, x[6] to "Kamita"),
            ),
        )
        val district = r.regions.single().districts.single()
        assertEquals("Sumbawanga CBD", district.name)
        assertEquals("551", district.code)
        assertEquals("Sumbawanga", district.wards.single().name)
        assertEquals(listOf("Katuma", "Kamita"), district.wards.single().mtaas.map { it.name })
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun wardCodeAboveItsName_isPairedWithTheNextWard() {
        val r = build(
            structure(
                *bHead,
                rowWithDistrict(130.0),
                line(146.0, x[5] to "63534"),
                line(162.0, x[4] to "Chikolopola", x[6] to "Chikoropola"),
            ),
        )
        val ward = r.regions.single().districts.single().wards.single()
        assertEquals("63534", ward.postcode)
        assertEquals("Chikolopola", ward.name)
        assertEquals(listOf("Chikoropola"), ward.mtaas.map { it.name })
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun wardCodeWithNoWardAfterIt_isFlagged() {
        val r = build(structure(*bHead, rowWithDistrict(130.0), line(146.0, x[5] to "63534"), line(162.0, x[0] to "MBEYA")))
        assertEquals(listOf(AnomalyKind.UNEXPECTED_CELL), r.anomalies.map { it.kind })
        assertEquals(2, r.anomalies.single().line)
    }

    @Test
    fun twoRegionsOnOnePage_rowsGoToTheirOwnRegion() {
        val g2 = geometryTwo
        val r = build(
            structure(
                banner(90.0, "ARUSHA REGION - 23000"), headerB(110.0),
                line(130.0, x[0] to "ARUSHA", x[1] to "23", x[2] to "MERU", x[3] to "231", x[4] to "W", x[5] to "23101", x[6] to "M"),
                banner(180.0, "DODOMA REGION - 41000"), headerB(200.0, g2),
                line(220.0, g2[0] to "DODOMA", g2[1] to "41", g2[2] to "CHAMWINO", g2[3] to "411", g2[4] to "W2", g2[5] to "41101", g2[6] to "M2"),
            ),
        )
        assertEquals(listOf("23000", "41000"), r.regions.map { it.code })
        assertEquals(listOf("M"), r.regions[0].districts.single().wards.single().mtaas.map { it.name })
        assertEquals(listOf("M2"), r.regions[1].districts.single().wards.single().mtaas.map { it.name })
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun secondHeaderMidPage_appliesItsColumnsToTheRowsBelowIt() {
        val g2 = geometryTwo
        val r = build(
            structure(
                *bHead,
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531", x[4] to "W1", x[5] to "53101", x[6] to "M1"),
                headerB(170.0, g2),
                line(190.0, g2[4] to "W2", g2[5] to "53102", g2[6] to "M2"),
            ),
        )
        val wards = r.regions.single().districts.single().wards
        assertEquals(listOf("W1", "W2"), wards.map { it.name })
        assertEquals(listOf("53101", "53102"), wards.map { it.postcode })
        assertTrue(r.anomalies.isEmpty(), r.anomalies.toString())
    }

    @Test
    fun shortAllCapsNames_areAcronymsForMtaaAndKitongojiButWordsForWards() {
        val r = build(
            structure(
                *bHead,
                line(130.0, x[0] to "MBEYA", x[1] to "53", x[2] to "MBEYA CBD", x[3] to "531", x[4] to "KIA", x[5] to "53101", x[6] to "NHC", x[7] to "CCM"),
            ),
        )
        val ward = r.regions.single().districts.single().wards.single()
        assertEquals("Kia", ward.name)
        assertEquals("NHC", ward.mtaas.single().name)
        assertEquals(listOf("CCM"), ward.mtaas.single().kitongojis)
    }
}
