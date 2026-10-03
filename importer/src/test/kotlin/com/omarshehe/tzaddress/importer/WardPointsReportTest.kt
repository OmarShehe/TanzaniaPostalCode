package com.omarshehe.tzaddress.importer

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class WardPointsReportTest {
    private val dataset = DatasetFixtures.dataset(
        listOf(
            RegionDto("11000", "Dar es Salaam", listOf(DistrictDto("11", "Ilala", listOf(DatasetFixtures.ward("11101", "Kivukoni"), DatasetFixtures.ward("11102", "Kariakoo"))))),
            RegionDto("71000", "Kusini Unguja", listOf(DistrictDto("71", "Kati", listOf(DatasetFixtures.ward("71101", "Jang'ombe"))))),
        ),
    )
    private val matches = listOf(
        WardMatch("11101", WardMatchKind.MATCHED_DISTRICT, WardPoint(-6.8, 39.3), ""),
        WardMatch("11102", WardMatchKind.AMBIGUOUS, null, "2 boundary wards with this name in the district"),
        WardMatch("71101", WardMatchKind.NO_MATCH, null, "no boundary ward with this name"),
    )

    @Test
    fun reportCountsTotalsPerRegionAndZanzibar() {
        val text = WardPointsReport.report(dataset, matches, 0.5, "the gate failed")
        assertContains(text, "Wards: 3")
        assertContains(text, "With a position: 1")
        assertContains(text, "| 11000 | Dar es Salaam | 2 | 1 |")
        assertContains(text, "| 71000 | Kusini Unguja | 1 | 0 |")
        assertContains(text, "Zanzibar wards with a position: 0 of 1")
        assertContains(text, "the gate failed")
    }

    @Test
    fun anomaliesCsvListsEveryWardWithoutAPointWithItsReason() {
        val lines = WardPointsReport.anomaliesCsv(dataset, matches).trim().lines()
        assertEquals("postcode,region,district,ward,kind,detail", lines[0])
        assertEquals(listOf("11102,Dar es Salaam,Ilala,Kariakoo,AMBIGUOUS,\"2 boundary wards with this name in the district\"", "71101,Kusini Unguja,Kati,Jang'ombe,NO_MATCH,\"no boundary ward with this name\""), lines.drop(1))
    }

    @Test
    fun reportSaysHowManyBoundaryFeaturesWereSkipped() {
        assertContains(WardPointsReport.report(dataset, matches, 0.5, null, skippedBoundaryFeatures = 4), "Boundary features skipped (no name or no usable geometry): 4")
        assertEquals(false, WardPointsReport.report(dataset, matches, 0.5, null).contains("skipped"))
    }
}
