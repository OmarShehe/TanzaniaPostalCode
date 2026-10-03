package com.omarshehe.tzaddress.importer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportOptionsTest {
    private fun parse(vararg args: String) = ImportOptions.parse(arrayOf(*args))

    @Test fun defaults_matchTheDocumentedPolicy() {
        val o = parse("--pdf=/tmp/a.pdf")
        assertEquals(File("/tmp/a.pdf"), o.pdf)
        assertEquals(30, o.policy.expectedRegions)
        assertEquals(0.005, o.policy.maxAnomalyRatio)
        assertFalse(o.force)
    }

    @Test fun missingPdf_isNull() = assertNull(parse().pdf)

    @Test fun policy_canBeOverridden() {
        val o = parse("--expectedRegions=99", "--maxAnomalyRatio=0.1")
        assertEquals(99, o.policy.expectedRegions)
        assertEquals(0.1, o.policy.maxAnomalyRatio)
    }

    @Test fun force_isABooleanNotJustPresence() {
        assertTrue(parse("--force").force)
        assertTrue(parse("--force=").force)
        assertTrue(parse("--force=true").force)
        assertFalse(parse("--force=false").force)
        assertFalse(parse("--force=0").force)
        assertFalse(parse("--force=no").force)
    }

    @Test fun valuesKeepEqualsSigns_includingTrailingOnes() {
        assertEquals(File("/tmp/a=b.pdf"), parse("--pdf=/tmp/a=b.pdf").pdf)
        assertEquals("edition=", parse("--sourceEdition=edition=").sourceEdition)
    }

    @Test fun blankOptionalValues_areNull() {
        val o = parse("--sourceEdition=", "--generatedAt=")
        assertNull(o.sourceEdition)
        assertNull(o.generatedAt)
    }

    @Test fun outDir_isResolvedAgainstRoot() {
        assertEquals(File("/repo/dataset"), parse("--root=/repo").outDir)
        assertEquals(File("/repo/out"), parse("--root=/repo", "--outDir=out").outDir)
        assertEquals(File("/abs/out"), parse("--root=/repo", "--outDir=/abs/out").outDir)
    }

    @Test fun nonNumericPolicyValues_failWithTheOptionNamed() {
        val regions = assertFailsWith<IllegalArgumentException> { parse("--expectedRegions=abc") }
        assertTrue("expectedRegions" in regions.message.orEmpty() && "abc" in regions.message.orEmpty())
        val ratio = assertFailsWith<IllegalArgumentException> { parse("--maxAnomalyRatio=lots") }
        assertTrue("maxAnomalyRatio" in ratio.message.orEmpty())
    }

    @Test fun defaultConstants_areTheDocumentedPolicy() {
        assertEquals(30, ImportOptions.DEFAULT_EXPECTED_REGIONS)
        assertEquals(0.005, ImportOptions.DEFAULT_MAX_ANOMALY_RATIO)
    }

    @Test fun pdfDir_defaultsExpectedRegionsToMainlandPlusZanzibar() {
        val o = parse("--pdfDir=/tmp/regions", "--pdf=/tmp/2012.pdf")
        assertEquals(File("/tmp/regions"), o.pdfDir)
        assertEquals(31, o.policy.expectedRegions)
        assertEquals(99, parse("--pdfDir=/tmp/r", "--expectedRegions=99").policy.expectedRegions)
    }

    @Test fun withoutPdfDir_theSingleFileDefaultStays() {
        assertNull(parse("--pdf=/tmp/a.pdf").pdfDir)
    }
}
