package com.omarshehe.tzaddress.importer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WardPointsOptionsTest {
    @Test
    fun parsesFilesAndDefaults() {
        val o = WardPointsOptions.parse(arrayOf("--wardBoundaries=/a/w.geojson", "--districtBoundaries=/a/d.geojson", "--root=/repo"))
        assertEquals(File("/a/w.geojson"), o.wardBoundaries)
        assertEquals(File("/a/d.geojson"), o.districtBoundaries)
        assertEquals(File("/repo", "dataset"), o.outDir)
        assertEquals(WardPointValidator.MIN_MATCH_RATIO, o.minMatchRatio)
        assertEquals(null, o.generatedAt)
    }

    @Test
    fun parsesRatioAndGeneratedAt() {
        val o = WardPointsOptions.parse(arrayOf("--minMatchRatio=0.9", "--generatedAt=2026-01-01T00:00:00Z", "--outDir=/x"))
        assertEquals(0.9, o.minMatchRatio)
        assertEquals("2026-01-01T00:00:00Z", o.generatedAt)
        assertEquals(File("/x"), o.outDir)
    }

    @Test
    fun rejectsANonNumericRatio() {
        assertFailsWith<IllegalArgumentException> { WardPointsOptions.parse(arrayOf("--minMatchRatio=lots")) }
    }
}
