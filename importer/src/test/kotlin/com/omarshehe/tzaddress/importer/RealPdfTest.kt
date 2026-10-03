package com.omarshehe.tzaddress.importer

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

/**
 * Opt-in checks against the real postcode PDF, which is not in the repo. Run with
 * `./gradlew :importer:test -Ppdf=/path/to/tzPostcodeList.pdf`; skipped otherwise.
 */
class RealPdfTest {
    private val pdf: File? = System.getenv("TZ_PDF")?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.isFile }

    private val policy = Policy(ImportOptions.DEFAULT_EXPECTED_REGIONS, ImportOptions.DEFAULT_MAX_ANOMALY_RATIO)
    private val info = InfoDto("1", "test", "2026-01-01T00:00:00Z")

    private fun parse(file: File) = ImportPipeline.parse(PdfWordExtractor.extract(file))

    @Test fun realPdf_passesTheFailurePolicy() {
        assumeTrue(pdf != null, "TZ_PDF not set: skipping real-PDF checks")
        val result = parse(pdf!!)
        val validation = Validator.validate(ImportPipeline.toDataset(result, info), result.suspectAnomalyCount, result.dataLineCount, policy)
        assertTrue(validation.passed, validation.violations.toString())
    }

    @Test fun wardCountsPerRegion_matchAnIndependentPdftotextCount() {
        assumeTrue(pdf != null, "TZ_PDF not set: skipping real-PDF checks")
        val independent = independentWardCounts(pdf!!)
        assumeTrue(independent != null, "pdftotext not available: skipping independent count")
        val parsed = parse(pdf).regions.associate { it.code to it.districts.sumOf { d -> d.wards.size } }
        assertEquals(independent, parsed)
    }

    @Test fun twoRuns_produceIdenticalJson() {
        assumeTrue(pdf != null, "TZ_PDF not set: skipping real-PDF checks")
        val first = DatasetWriter.toJson(ImportPipeline.toDataset(parse(pdf!!), info))
        val second = DatasetWriter.toJson(ImportPipeline.toDataset(parse(pdf), info))
        assertEquals(first, second)
    }

    /** Ward postcodes are the only 5-digit tokens outside region banners; count them per region straight from `pdftotext -layout`. */
    private fun independentWardCounts(file: File): Map<String, Int>? {
        val text = try {
            val process = ProcessBuilder("pdftotext", "-layout", file.absolutePath, "-").redirectErrorStream(true).start()
            val out = process.inputStream.bufferedReader().readText()
            if (!process.waitFor(120, TimeUnit.SECONDS) || process.exitValue() != 0) return null
            out
        } catch (e: java.io.IOException) {
            return null
        }
        val banner = Regex("(\\S.*?)\\s+REGION\\s*[\\u2010-\\u2015-]\\s*(\\d{5})\\s*$")
        val code = Regex("(^|\\s)\\d{5}(\\s|$)")
        val counts = LinkedHashMap<String, Int>()
        var current: String? = null
        for (line in text.lineSequence()) {
            val match = banner.find(line)
            if (match != null) {
                current = match.groupValues[2]
                counts[current] = 0
            } else if (current != null && code.containsMatchIn(line)) {
                counts[current] = counts.getValue(current) + 1
            }
        }
        return counts
    }
}
