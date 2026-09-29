package com.omarshehe.tzaddress.importer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Checks the committed `dataset/tz-address.json` against the failure policy and against the `:core` model. */
class CommittedDatasetTest {
    private val file = File("../dataset/tz-address.json")
    private val dataset by lazy { DatasetWriter.fromJson(file.readText()) }

    @Test fun datasetExists() = assertTrue(file.isFile, "run :importer:importPostcodes and commit dataset/")

    @Test fun datasetPassesValidation() {
        val result = Validator.validate(dataset, 0, 1, Policy(ImportOptions.DEFAULT_EXPECTED_REGIONS, ImportOptions.DEFAULT_MAX_ANOMALY_RATIO))
        assertTrue(result.passed, result.violations.toString())
    }

    @Test fun mapsToCoreWithEveryParentResolving() {
        val core = dataset.toCore()
        val regionCodes = core.regions.map { it.code }.toSet()
        val districtCodes = core.districts.map { it.code }.toSet()
        val wardCodes = core.wards.map { it.postcode }.toSet()
        val mtaaIds = core.mtaas.map { it.id }.toSet()
        assertTrue(core.districts.all { it.regionCode in regionCodes })
        assertTrue(core.wards.all { it.districtCode in districtCodes })
        assertTrue(core.wards.all { it.postcode.startsWith(it.districtCode) })
        assertTrue(core.mtaas.all { it.wardPostcode in wardCodes })
        assertTrue(core.kitongojis.all { it.mtaaId in mtaaIds })
    }

    @Test fun idsAreUnique() {
        val core = dataset.toCore()
        assertEquals(core.mtaas.size, core.mtaas.map { it.id }.toSet().size)
        assertEquals(core.kitongojis.size, core.kitongojis.map { it.id }.toSet().size)
        assertEquals(core.wards.size, core.wards.map { it.postcode }.toSet().size)
    }

    @Test fun zanzibarRegionsAreAllPresent() {
        val names = dataset.regions.map { it.name }.toSet()
        listOf("Mjini Magharibi", "Kusini Unguja", "Kaskazini Unguja", "Kusini Pemba", "Kaskazini Pemba").forEach {
            assertTrue(it in names, "missing $it")
        }
    }
}
