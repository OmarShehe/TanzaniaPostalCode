package com.omarshehe.tzaddress.importer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WardPointsDatasetTest {
    private fun withPoint() = DatasetDto(
        InfoDto("2", "test", "2026-01-01T00:00:00Z", "© OpenStreetMap contributors"),
        listOf(
            RegionDto(
                "11000", "Dar es Salaam",
                listOf(
                    DistrictDto(
                        "11", "Ilala",
                        listOf(
                            WardDto("11101", "Kivukoni", emptyList(), -6.81234, 39.28765),
                            WardDto("11102", "Kariakoo", emptyList()),
                        ),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun pointsAndAttributionRoundTripThroughJson() {
        val back = DatasetWriter.fromJson(DatasetWriter.toJson(withPoint()))
        val wards = back.regions.single().districts.single().wards
        assertEquals(-6.81234, wards[0].latitude)
        assertEquals(39.28765, wards[0].longitude)
        assertNull(wards[1].latitude)
        assertNull(wards[1].longitude)
        assertEquals("© OpenStreetMap contributors", back.info.attribution)
    }

    @Test
    fun jsonWrittenBeforeCoordinatesStillReads() {
        val old = """{"info":{"version":"1","sourceEdition":"x","generatedAt":"t"},"regions":[{"code":"11000","name":"D","districts":[{"code":"11","name":"I","wards":[{"postcode":"11101","name":"K","mtaas":[]}]}]}]}"""
        val ward = DatasetWriter.fromJson(old).regions.single().districts.single().wards.single()
        assertNull(ward.latitude)
        assertNull(ward.longitude)
    }

    @Test
    fun toCoreCarriesPointsAndAttribution() {
        val core = withPoint().toCore()
        assertEquals(-6.81234, core.wards.first { it.postcode == "11101" }.latitude)
        assertEquals(39.28765, core.wards.first { it.postcode == "11101" }.longitude)
        assertNull(core.wards.first { it.postcode == "11102" }.latitude)
        assertEquals("© OpenStreetMap contributors", core.info.attribution)
    }
}
