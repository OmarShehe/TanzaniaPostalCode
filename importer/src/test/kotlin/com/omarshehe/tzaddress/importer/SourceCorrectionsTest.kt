package com.omarshehe.tzaddress.importer

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceCorrectionsTest {

    private fun result(code: String, name: String) =
        BuildResult(listOf(RegionNode("59000", "Njombe", mutableListOf(DistrictNode(code, name)))), emptyList(), 1)

    @Test fun aDistrictNameBrokenMidWordBySourceWrapping_isJoined() {
        val r = result("593", "WANGING'O MBE")
        val notes = SourceCorrections.apply(r)
        assertEquals("Wanging'ombe", r.regions.single().districts.single().name)
        assertEquals(1, notes.size)
    }

    @Test fun otherDistricts_areLeftAlone() {
        val r = result("593", "Wanging'ombe")
        assertEquals(emptyList(), SourceCorrections.apply(r))
        val other = result("591", "Njombe")
        assertEquals(emptyList(), SourceCorrections.apply(other))
    }

    @Test fun aHyphenBeforeCbd_isDropped() {
        val r = result("501", "MPANDA -CBD")
        SourceCorrections.apply(r)
        assertEquals("Mpanda CBD", r.regions.single().districts.single().name)
    }
}
