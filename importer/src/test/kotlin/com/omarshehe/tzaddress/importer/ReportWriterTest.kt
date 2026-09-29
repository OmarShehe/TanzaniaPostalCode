package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.importer.DatasetFixtures.valid
import kotlin.test.Test
import kotlin.test.assertTrue

class ReportWriterTest {
    private val policy = Policy(expectedRegions = 2, maxAnomalyRatio = 0.01)
    private val anomaly = Anomaly(12, 3, AnomalyKind.BAD_WARD_POSTCODE, "Ward, \"One\" 5310")

    @Test fun report_containsCountsAndVerdict() {
        val validation = Validator.validate(valid(), 0, 100, policy)
        val md = ReportWriter.report(valid(), validation, emptyList(), 100)
        assertTrue("PASSED" in md)
        assertTrue("| 11000 | Dar es Salaam |" in md)
        assertTrue("Regions: 2" in md)
    }

    @Test fun failedReport_namesTheViolation() {
        val ds = DatasetFixtures.dataset(listOf(RegionDto("53000", "Mbeya", listOf(DistrictDto("531", "A", listOf(DatasetFixtures.ward("53101"), DatasetFixtures.ward("53101")))))))
        val validation = Validator.validate(ds, 0, 100, policy)
        val md = ReportWriter.report(ds, validation, emptyList(), 100)
        assertTrue("FAILED" in md)
        assertTrue("53101" in md)
    }

    @Test fun anomalyCsv_quotesFieldsAndHasHeader() {
        val csv = ReportWriter.anomaliesCsv(listOf(anomaly))
        val lines = csv.trimEnd().lines()
        assertTrue(lines[0] == "page,line,kind,text")
        assertTrue(lines[1] == "12,3,BAD_WARD_POSTCODE,\"Ward, \"\"One\"\" 5310\"")
    }

    @Test fun report_listsAtMostFiftyAnomalies() {
        val many = (1..80).map { Anomaly(it, 1, AnomalyKind.ORPHAN_ROW, "x$it") }
        val md = ReportWriter.report(valid(), Validator.validate(valid(), 80, 100000, policy), many, 100000)
        assertTrue("x50" in md)
        assertTrue("x51" !in md)
    }
}
