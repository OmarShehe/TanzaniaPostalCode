package com.omarshehe.tzaddress.importer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WardPointsMergeTest {
    private fun dataset(attribution: String, vararg wards: WardDto) = DatasetDto(
        InfoDto("2", "e", "t", attribution),
        listOf(RegionDto("11000", "R", listOf(DistrictDto("11", "D", wards.toList())))),
    )

    private fun wardsOf(d: DatasetDto) = d.regions.single().districts.single().wards.associateBy { it.postcode }

    @Test
    fun carryOverCopiesPointsByPostcodeAndLeavesUnknownWardsEmpty() {
        val existing = dataset("© OSM", WardDto("11101", "A", emptyList(), -6.1, 39.1), WardDto("11102", "B", emptyList()))
        val fresh = dataset("", WardDto("11101", "A renamed", emptyList()), WardDto("11102", "B", emptyList()), WardDto("11103", "C", emptyList()))
        val merged = CarryOverPoints.merge(fresh, existing)
        val wards = wardsOf(merged)
        assertEquals(-6.1, wards.getValue("11101").latitude)
        assertEquals(39.1, wards.getValue("11101").longitude)
        assertNull(wards.getValue("11102").latitude)
        assertNull(wards.getValue("11103").latitude)
        assertEquals("© OSM", merged.info.attribution)
    }

    @Test
    fun carryOverKeepsTheFreshAttributionWhenNoPointCarries() {
        val existing = dataset("© OSM", WardDto("11101", "A", emptyList()))
        val fresh = dataset("", WardDto("11101", "A", emptyList()))
        assertEquals("", CarryOverPoints.merge(fresh, existing).info.attribution)
        assertEquals("", CarryOverPoints.merge(fresh, null).info.attribution)
    }

    @Test
    fun applySetsPointsOnlyForMatchesAndClearsTheRest() {
        val data = dataset("", WardDto("11101", "A", emptyList(), -5.0, 35.0), WardDto("11102", "B", emptyList()))
        val matches = listOf(
            WardMatch("11101", WardMatchKind.NO_MATCH, null, "x"),
            WardMatch("11102", WardMatchKind.MATCHED_DISTRICT, WardPoint(-6.5, 39.25), ""),
        )
        val result = WardPointsApplier.apply(data, matches, "© OSM", "2")
        val wards = wardsOf(result)
        assertNull(wards.getValue("11101").latitude)
        assertNull(wards.getValue("11101").longitude)
        assertEquals(-6.5, wards.getValue("11102").latitude)
        assertEquals(39.25, wards.getValue("11102").longitude)
        assertEquals("© OSM", result.info.attribution)
        assertEquals("2", result.info.version)
    }
}
