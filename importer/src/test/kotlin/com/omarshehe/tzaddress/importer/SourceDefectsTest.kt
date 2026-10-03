package com.omarshehe.tzaddress.importer

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceDefectsTest {

    private fun result(): BuildResult {
        val district = DistrictNode("537", "Rungwe", mutableListOf(WardNode("53701", "Good"), WardNode("73733", "Njisi"), WardNode("5371", "Short")))
        return BuildResult(listOf(RegionNode("53000", "Mbeya", mutableListOf(district))), emptyList(), 3)
    }

    @Test fun wardsWithABadPostcodeOrADifferentDistrictPrefix_areLeftOut() {
        val r = result()
        val defects = SourceDefects.remove(r)
        assertEquals(listOf("53701"), r.regions.single().districts.single().wards.map { it.postcode })
        assertEquals(2, defects.size)
        assertEquals(true, defects.any { "73733" in it && "Njisi" in it && "537" in it })
        assertEquals(true, defects.any { "5371" in it && "Short" in it })
    }

    @Test fun cleanData_isUntouched() {
        val r = BuildResult(listOf(RegionNode("53000", "Mbeya", mutableListOf(DistrictNode("537", "R", mutableListOf(WardNode("53701", "A")))))), emptyList(), 1)
        assertEquals(emptyList(), SourceDefects.remove(r))
    }
}
