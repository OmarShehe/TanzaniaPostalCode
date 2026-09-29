package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.importer.DatasetFixtures.dataset
import com.omarshehe.tzaddress.importer.DatasetFixtures.valid
import com.omarshehe.tzaddress.importer.DatasetFixtures.ward
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidatorTest {
    private val policy = Policy(expectedRegions = 2, maxAnomalyRatio = 0.01)

    private fun kinds(d: DatasetDto, anomalies: Int = 0, lines: Int = 1000) =
        Validator.validate(d, anomalies, lines, policy).violations.map { it.kind }

    @Test fun cleanData_passes() {
        val result = Validator.validate(valid(), 0, 1000, policy)
        assertTrue(result.passed, result.violations.toString())
    }

    @Test fun duplicateWardPostcode_failsAndNamesTheCode() {
        val d = dataset(
            listOf(
                RegionDto("53000", "Mbeya", listOf(DistrictDto("531", "A", listOf(ward("53101"), ward("53101"))))),
                RegionDto("11000", "Dar", listOf(DistrictDto("11", "B", listOf(ward("11101"))))),
            ),
        )
        val result = Validator.validate(d, 0, 1000, policy)
        assertEquals(listOf(ViolationKind.DUPLICATE_WARD_POSTCODE), result.violations.map { it.kind })
        assertTrue("53101" in result.violations.single().message)
    }

    @Test fun wardPostcodeNotFiveDigits_fails() {
        val d = dataset(
            listOf(
                RegionDto("53000", "Mbeya", listOf(DistrictDto("531", "A", listOf(ward("5310"))))),
                RegionDto("11000", "Dar", listOf(DistrictDto("11", "B", listOf(ward("11101"))))),
            ),
        )
        assertTrue(ViolationKind.BAD_WARD_POSTCODE in kinds(d))
    }

    @Test fun wardPrefixNotMatchingDistrictCode_fails() {
        val d = dataset(
            listOf(
                RegionDto("53000", "Mbeya", listOf(DistrictDto("531", "A", listOf(ward("54101"))))),
                RegionDto("11000", "Dar", listOf(DistrictDto("11", "B", listOf(ward("11101"))))),
            ),
        )
        assertEquals(listOf(ViolationKind.WARD_PREFIX_MISMATCH), kinds(d))
    }

    @Test fun duplicateDistrictCodeAcrossRegions_fails() {
        val d = dataset(
            listOf(
                RegionDto("53000", "Mbeya", listOf(DistrictDto("531", "A", listOf(ward("53101"))))),
                RegionDto("54000", "Other", listOf(DistrictDto("531", "B", listOf(ward("53102"))))),
            ),
        )
        assertTrue(ViolationKind.DUPLICATE_DISTRICT_CODE in kinds(d))
    }

    @Test fun wrongRegionCount_fails() {
        val d = dataset(listOf(RegionDto("53000", "Mbeya", listOf(DistrictDto("531", "A", listOf(ward("53101")))))))
        assertEquals(listOf(ViolationKind.REGION_COUNT), kinds(d))
    }

    @Test fun anomalyRatioAboveThreshold_fails() {
        assertEquals(listOf(ViolationKind.ANOMALY_RATIO), kinds(valid(), anomalies = 11, lines = 1000))
        assertTrue(kinds(valid(), anomalies = 10, lines = 1000).isEmpty())
    }

    @Test fun districtWithoutWards_andWardWithoutMtaas_areWarningsNotFailures() {
        val d = dataset(
            listOf(
                RegionDto("53000", "Mbeya", listOf(DistrictDto("531", "A", emptyList()), DistrictDto("532", "B", listOf(ward("53201", mtaas = emptyList()))))),
                RegionDto("11000", "Dar", listOf(DistrictDto("11", "C", listOf(ward("11101"))))),
            ),
        )
        val result = Validator.validate(d, 0, 1000, policy)
        assertTrue(result.passed)
        assertEquals(setOf(WarningKind.DISTRICT_WITHOUT_WARDS, WarningKind.WARD_WITHOUT_MTAAS), result.warnings.map { it.kind }.toSet())
    }

    @Test fun absentSongwe_isReportedAsInformationOnly() {
        val result = Validator.validate(valid(), 0, 1000, policy)
        assertTrue(result.notes.any { "Songwe" in it })
        assertTrue(result.passed)
    }

    @Test fun emptyOrMalformedDistrictCode_fails() {
        listOf("", "5", "5310", "ab").forEach { code ->
            val d = dataset(
                listOf(
                    RegionDto("53000", "Mbeya", listOf(DistrictDto(code, "A", listOf(ward("53101"))))),
                    RegionDto("11000", "Dar", listOf(DistrictDto("11", "B", listOf(ward("11101"))))),
                ),
            )
            assertTrue(ViolationKind.BAD_DISTRICT_CODE in kinds(d), "code '$code'")
        }
    }
}
