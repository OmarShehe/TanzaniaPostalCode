package com.omarshehe.tzaddress.importer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WardPointValidatorTest {
    private fun data(vararg wards: WardDto) =
        DatasetFixtures.dataset(listOf(RegionDto("11000", "R", listOf(DistrictDto("11", "D", wards.toList())))))

    private fun ward(postcode: String, lat: Double?, lon: Double?) = WardDto(postcode, "W$postcode", emptyList(), lat, lon)

    private fun kinds(dataset: DatasetDto) = WardPointValidator.check(dataset).map { it.kind }

    @Test fun noPointsIsValid() = assertEquals(emptyList(), kinds(data(ward("11101", null, null))))

    @Test fun halfAPairIsRejected() {
        assertEquals(listOf(ViolationKind.WARD_POINT_HALF), kinds(data(ward("11101", -6.0, null))))
        assertEquals(listOf(ViolationKind.WARD_POINT_HALF), kinds(data(ward("11101", null, 39.0))))
    }

    @Test fun boundingBoxEdgesAreInsideAndJustOutsideIsRejected() {
        assertEquals(emptyList(), kinds(data(ward("11101", -12.0, 29.0), ward("11102", -1.0, 41.5))))
        listOf(-12.00001 to 35.0, -0.99999 to 35.0, -6.0 to 28.99999, -6.0 to 41.50001).forEach { (lat, lon) ->
            assertEquals(listOf(ViolationKind.WARD_POINT_OUT_OF_BOUNDS), kinds(data(ward("11101", lat, lon))), "$lat,$lon")
        }
    }

    @Test fun coverageAtTheMinimumPassesAndBelowFails() {
        val wards = (1..20).map { ward("111%02d".format(it), if (it <= 17) -6.0 else null, if (it <= 17) 39.0 else null) }
        assertEquals(null, WardPointValidator.coverage(data(*wards.toTypedArray()), 0.85))
        val fewer = wards.mapIndexed { i, w -> if (i == 0) ward(w.postcode, null, null) else w }
        val violation = WardPointValidator.coverage(data(*fewer.toTypedArray()), 0.85)
        assertEquals(ViolationKind.WARD_POINT_COVERAGE, violation?.kind)
        assertTrue(violation!!.message.contains("16 of 20"))
    }

    @Test fun datasetValidatorAlsoRunsThePointChecks() {
        val bad = data(ward("11101", -6.0, null))
        val result = Validator.validate(bad, 0, 1, Policy(1, 0.5))
        assertTrue(result.violations.any { it.kind == ViolationKind.WARD_POINT_HALF })
    }
}
