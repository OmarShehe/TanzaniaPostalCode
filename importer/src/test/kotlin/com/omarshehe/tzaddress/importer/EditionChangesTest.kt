package com.omarshehe.tzaddress.importer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditionChangesTest {

    private fun ward(code: String, name: String) = WardDto(code, name, emptyList())

    private fun data(vararg regions: RegionDto) = DatasetDto(InfoDto("2", "x", "t"), regions.toList())

    private fun region(code: String, name: String, vararg districts: DistrictDto) = RegionDto(code, name, districts.toList())

    private val previous = data(
        region("11000", "Dar es Salaam", DistrictDto("11", "Ilala", listOf(ward("11101", "Kivukoni"), ward("11102", "Kariakoo"), ward("11103", "Gone")))),
        region("71000", "Mjini Magharibi", DistrictDto("71", "Mjini", listOf(ward("71101", "Zanzibar Ward")))),
    )

    private val current = data(
        region(
            "11000", "Dar es Salaam",
            DistrictDto("11", "Ilala", listOf(ward("11101", "Kivukoni"), ward("11122", "Kariakoo"), ward("11105", "New Ward"))),
            DistrictDto("17", "Kigamboni", listOf(ward("17101", "Kigamboni"))),
        ),
        region("54100", "Songwe", DistrictDto("541", "Songwe", listOf(ward("54101", "Gua")))),
        region("71000", "Mjini Magharibi", DistrictDto("71", "Mjini", listOf(ward("71101", "Zanzibar Ward")))),
    )

    private val changes = EditionChanges.compare(previous, current, mapOf("11122" to "11102"))

    @Test fun wardWithAnOldPostcodeInThePreviousList_isRecoded() {
        assertEquals(listOf("11102" to "11122"), changes.recoded.map { it.oldPostcode to it.newPostcode })
    }

    @Test fun wardsInNeitherPostcodeNorOldPostcode_areAddedOrRemoved() {
        assertEquals(setOf("11105", "17101", "54101"), changes.added.map { it.postcode }.toSet())
        assertEquals(listOf("11103"), changes.removed.map { it.postcode })
    }

    @Test fun sameCodeWithAnotherName_isRenamed() {
        val renamed = EditionChanges.compare(previous, data(region("11000", "Dar es Salaam", DistrictDto("11", "Ilala", listOf(ward("11101", "Kivukoni Mpya"))))), emptyMap())
        assertEquals(listOf(Triple("11101", "Kivukoni", "Kivukoni Mpya")), renamed.renamed.map { Triple(it.postcode, it.oldName, it.newName) })
    }

    @Test fun newRegionsAndDistrictsAreListed() {
        assertEquals(listOf("54100"), changes.addedRegions.map { it.code })
        assertEquals(listOf("17", "541"), changes.addedDistricts.map { it.code }.sorted())
    }

    @Test fun unchangedRegionsReportNothing() {
        assertTrue(changes.added.none { it.regionCode == "71000" })
        assertTrue(changes.removed.none { it.regionCode == "71000" })
    }

    @Test fun reportShowsCountsPerRegionAndIsIdenticalOnASecondRun() {
        val first = EditionChangesReport.markdown(changes, "2012-07-30", "2016-04-22 (Gazette Notice 240)")
        val second = EditionChangesReport.markdown(EditionChanges.compare(previous, current, mapOf("11122" to "11102")), "2012-07-30", "2016-04-22 (Gazette Notice 240)")
        assertEquals(first, second)
        assertTrue("Songwe" in first && "11122" in first && "11103" in first, first)
        assertTrue("| Dar es Salaam | 11000 |" in first, first)
    }

    @Test fun anOldPostcodeUsedByTwoWards_isReportedOnce() {
        val dup = EditionChanges.duplicateOldPostcodes(mapOf("11122" to "11102", "11123" to "11102", "11124" to "11105"))
        assertEquals(mapOf("11102" to listOf("11122", "11123")), dup)
    }
}
