package com.omarshehe.tzaddress.importer

import org.locationtech.jts.geom.Envelope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WardPointJoinTest {
    /** A 1x1 degree square whose south-west corner is ([lon], [lat]); its interior point is the middle. */
    private fun square(name: String, lon: Double, lat: Double, size: Double = 1.0) =
        BoundaryFeature(name, GeoJsonBoundaries.factory.toGeometry(Envelope(lon, lon + size, lat, lat + size)))

    private fun dataset(vararg districts: Pair<String, List<Pair<String, String>>>) = DatasetFixtures.dataset(
        listOf(
            RegionDto(
                "10000", "R",
                districts.mapIndexed { i, (districtName, wards) ->
                    DistrictDto("1${i + 1}", districtName, wards.map { (postcode, name) -> DatasetFixtures.ward(postcode, name) })
                },
            ),
        ),
    )

    private val districtA = square("Alpha", 30.0, -5.0, 3.0)
    private val districtB = square("Beta", 34.0, -5.0, 3.0)

    private fun kinds(matches: List<WardMatch>) = matches.associate { it.postcode to it.kind }

    @Test
    fun matchesByDistrictAndName() {
        val data = dataset("Alpha" to listOf("11101" to "Kati"), "Beta" to listOf("12101" to "Kati"))
        val matches = WardPointJoin.join(data, listOf(square("Kati", 31.0, -4.0), square("Kati", 35.0, -4.0)), listOf(districtA, districtB))
        assertEquals(mapOf("11101" to WardMatchKind.MATCHED_DISTRICT, "12101" to WardMatchKind.MATCHED_DISTRICT), kinds(matches))
        assertEquals(WardPoint(-3.5, 31.5), matches.first { it.postcode == "11101" }.point)
        assertEquals(WardPoint(-3.5, 35.5), matches.first { it.postcode == "12101" }.point)
    }

    @Test
    fun ignoresCaseSpacingPunctuationAndSourceNumbering() {
        val data = dataset("Alpha" to listOf("11101" to "60. Kaseme A Mabamba"))
        val matches = WardPointJoin.join(data, listOf(square("KASEME A-MABAMBA", 31.0, -4.0)), listOf(districtA))
        assertEquals(WardMatchKind.MATCHED_DISTRICT, matches.single().kind)
    }

    @Test
    fun fallsBackToAUniqueNameWhenTheDistrictNamesDiffer() {
        val data = dataset("Ilala Cbd" to listOf("11101" to "Kivukoni"))
        val matches = WardPointJoin.join(data, listOf(square("Kivukoni", 31.0, -4.0)), listOf(districtA))
        assertEquals(WardMatchKind.MATCHED_UNIQUE_NAME, matches.single().kind)
        assertNotNull(matches.single().point)
    }

    @Test
    fun uniqueNameFallbackIsRefusedWhenTheBoundaryWardIsInADistrictOfAnotherRegion() {
        // The only boundary "Kati" lies in district Beta, which we have only in region R2; our "Kati" is in region R1.
        val data = DatasetFixtures.dataset(
            listOf(
                RegionDto("10000", "R1", listOf(DistrictDto("11", "Alpha", listOf(DatasetFixtures.ward("11101", "Kati"))))),
                RegionDto("20000", "R2", listOf(DistrictDto("21", "Beta", listOf(DatasetFixtures.ward("21101", "Other"))))),
            ),
        )
        val matches = WardPointJoin.join(data, listOf(square("Kati", 35.0, -4.0)), listOf(districtA, districtB))
        val kati = matches.first { it.postcode == "11101" }
        assertEquals(WardMatchKind.NO_MATCH, kati.kind)
        assertNull(kati.point)
        assertEquals("the only boundary ward with this name is in district Beta, which the list has only in another region", kati.detail)
    }

    @Test
    fun uniqueNameFallbackStillWorksForASiblingDistrictInTheSameRegion() {
        // Our list splits "Ilala" into "Ilala Cbd" and "Ilala"; the boundary data has one "Ilala" polygon.
        val data = dataset("Ilala Cbd" to listOf("11101" to "Kivukoni"), "Alpha" to listOf("12101" to "Other"))
        val matches = WardPointJoin.join(data, listOf(square("Kivukoni", 31.0, -4.0)), listOf(districtA))
        assertEquals(WardMatchKind.MATCHED_UNIQUE_NAME, matches.first { it.postcode == "11101" }.kind)
    }

    @Test
    fun sameNameTwiceInOneDistrictIsAmbiguous() {
        val data = dataset("Alpha" to listOf("11101" to "Kati"))
        val matches = WardPointJoin.join(data, listOf(square("Kati", 30.5, -4.0, 0.4), square("Kati", 31.5, -4.0, 0.4)), listOf(districtA))
        assertEquals(WardMatchKind.AMBIGUOUS, matches.single().kind)
        assertNull(matches.single().point)
    }

    @Test
    fun aNameWithNoBoundaryIsNoMatch() {
        val data = dataset("Alpha" to listOf("11101" to "Nowhere"))
        val matches = WardPointJoin.join(data, listOf(square("Kati", 31.0, -4.0)), listOf(districtA))
        assertEquals(WardMatchKind.NO_MATCH, matches.single().kind)
        assertNull(matches.single().point)
    }

    @Test
    fun anUnplacedBoundaryWardWithADuplicatedNameOnOurSideIsAmbiguous() {
        // One boundary "Kati" outside every district polygon; two wards of ours share the name.
        val data = dataset("Alpha" to listOf("11101" to "Kati"), "Beta" to listOf("12101" to "Kati"))
        val matches = WardPointJoin.join(data, listOf(square("Kati", 50.0, 5.0)), listOf(districtA, districtB))
        assertEquals(mapOf("11101" to WardMatchKind.AMBIGUOUS, "12101" to WardMatchKind.AMBIGUOUS), kinds(matches))
    }

    @Test
    fun oneBoundaryWardClaimedByTwoOfOursMakesBothAmbiguous() {
        // Two district names that normalise alike would both claim the single boundary ward through the fallback; here the
        // same boundary ward is the only "Kati" in district Alpha and both our wards are in Alpha.
        val data = dataset("Alpha" to listOf("11101" to "Kati", "11102" to "KATI"))
        val matches = WardPointJoin.join(data, listOf(square("Kati", 31.0, -4.0)), listOf(districtA))
        assertEquals(mapOf("11101" to WardMatchKind.AMBIGUOUS, "11102" to WardMatchKind.AMBIGUOUS), kinds(matches))
        assertEquals(listOf(null, null), matches.map { it.point })
    }

    @Test
    fun resultIsInPostcodeOrderAndIdenticalOnARepeatRun() {
        val data = dataset("Beta" to listOf("12102" to "Zeta", "12101" to "Alfa"), "Alpha" to listOf("11101" to "Kati"))
        val wards = listOf(square("Zeta", 35.0, -4.0), square("Alfa", 36.0, -4.0), square("Kati", 31.0, -4.0))
        val first = WardPointJoin.join(data, wards, listOf(districtA, districtB))
        assertEquals(listOf("11101", "12101", "12102"), first.map { it.postcode })
        assertEquals(first, WardPointJoin.join(data, wards.reversed(), listOf(districtB, districtA)))
    }

    @Test
    fun aSpellingVariantInTheSameDistrictMatchesAndIsMarkedSimilar() {
        val data = dataset("Alpha" to listOf("11101" to "Mikinguni"))
        val match = WardPointJoin.join(data, listOf(square("Mikunguni", 31.0, -4.0)), listOf(districtA)).single()
        assertEquals(WardMatchKind.MATCHED_SIMILAR_NAME, match.kind)
        assertEquals(WardPoint(-3.5, 31.5), match.point)
        assertEquals("similar to boundary ward 'Mikunguni' (0.89)", match.detail)
    }

    @Test
    fun aSpellingVariantOnlyInAnotherDistrictStaysNoMatch() {
        val data = dataset("Alpha" to listOf("11101" to "Mikinguni"))
        val match = WardPointJoin.join(data, listOf(square("Mikunguni", 35.0, -4.0)), listOf(districtA, districtB)).single()
        assertEquals(WardMatchKind.NO_MATCH, match.kind)
        assertNull(match.point)
    }

    @Test
    fun twoEquallyCloseBoundaryWardsLeaveItUnmatched() {
        val data = dataset("Alpha" to listOf("11101" to "Mwakasumbe"))
        val boundaries = listOf(square("Mwakasumba", 30.5, -4.0, 0.4), square("Mwakasumbo", 31.5, -4.0, 0.4))
        assertEquals(WardMatchKind.NO_MATCH, WardPointJoin.join(data, boundaries, listOf(districtA)).single().kind)
    }

    @Test
    fun twoWardsEquallyCloseToOneBoundaryWardBothStayUnmatched() {
        val data = dataset("Alpha" to listOf("11101" to "Mikinguni", "11102" to "Mikanguni"))
        val matches = WardPointJoin.join(data, listOf(square("Mikunguni", 31.0, -4.0)), listOf(districtA))
        assertEquals(mapOf("11101" to WardMatchKind.NO_MATCH, "11102" to WardMatchKind.NO_MATCH), kinds(matches))
    }

    @Test
    fun aBoundaryWardAlreadyTakenByAnExactNameIsNotReused() {
        val data = dataset("Alpha" to listOf("11101" to "Mikunguni", "11102" to "Mikinguni"))
        val matches = WardPointJoin.join(data, listOf(square("Mikunguni", 31.0, -4.0)), listOf(districtA))
        assertEquals(mapOf("11101" to WardMatchKind.MATCHED_DISTRICT, "11102" to WardMatchKind.NO_MATCH), kinds(matches))
    }

    @Test
    fun aDifferentFirstLetterIsNotAVariant() {
        val data = dataset("Alpha" to listOf("11101" to "Bwakasumbe"))
        assertEquals(WardMatchKind.NO_MATCH, WardPointJoin.join(data, listOf(square("Mwakasumbe", 31.0, -4.0)), listOf(districtA)).single().kind)
    }

    @Test
    fun anAmbiguousWardIsNotRetriedAsASpellingVariant() {
        val data = dataset("Alpha" to listOf("11101" to "Kati"))
        val matches = WardPointJoin.join(data, listOf(square("Kati", 30.5, -4.0, 0.4), square("Kati", 31.5, -4.0, 0.4)), listOf(districtA))
        assertEquals(WardMatchKind.AMBIGUOUS, matches.single().kind)
    }

    @Test
    fun aWardWithASplitSuffixIsNotMatchedToItsParentBoundary() {
        // "Matale A" and "Kitama 1" look like later splits of a ward; the boundary data has only the parent polygon.
        val data = dataset("Alpha" to listOf("11101" to "Matale A", "11102" to "Kitama 1"))
        val boundaries = listOf(square("Matale", 30.5, -4.0, 0.4), square("Kitama", 31.5, -4.0, 0.4))
        assertEquals(mapOf("11101" to WardMatchKind.NO_MATCH, "11102" to WardMatchKind.NO_MATCH), kinds(WardPointJoin.join(data, boundaries, listOf(districtA))))
    }
}
