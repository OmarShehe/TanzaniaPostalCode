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

    private val wards by lazy { dataset.regions.flatMap { r -> r.districts.flatMap { d -> d.wards.map { w -> r to w } } } }

    @Test fun wardPositionsAreValidAndCoverEnoughWards() {
        assertEquals(emptyList(), WardPointValidator.check(dataset))
        assertEquals(null, WardPointValidator.coverage(dataset, WardPointValidator.MIN_MATCH_RATIO))
    }

    @Test fun wardWhoseOnlySameNamedBoundaryIsInAnotherDistrictGetsNoPosition() {
        // Ugalla (Mlele, Katavi): the only boundary ward called Ugalla is in Urambo, Tabora, 42 km away.
        val ugalla = wards.single { (_, w) -> w.postcode == "50313" }.second
        assertEquals(null, ugalla.latitude)
        assertEquals(null, ugalla.longitude)
    }

    @Test fun datasetWithPositionsCarriesTheOpenStreetMapNotice() {
        assertTrue(wards.any { (_, w) -> w.latitude != null })
        assertTrue("OpenStreetMap" in dataset.info.attribution && "ODbL" in dataset.info.attribution, dataset.info.attribution)
        assertEquals(DatasetVersion.CURRENT, dataset.info.version)
    }

    @Test fun zanzibarAndDarEsSalaamPositionsAreInTheirAreas() {
        val zanzibar = setOf("Mjini Magharibi", "Kusini Unguja", "Kaskazini Unguja", "Kusini Pemba", "Kaskazini Pemba")
        val islands = wards.filter { (r, w) -> r.name in zanzibar && w.latitude != null }
        assertTrue(islands.isNotEmpty())
        assertTrue(islands.all { (_, w) -> w.latitude!! in -6.5..-4.8 && w.longitude!! in 39.1..39.9 })
        val dar = wards.filter { (r, w) -> r.name == "Dar es Salaam" && w.latitude != null }
        assertTrue(dar.isNotEmpty())
        assertTrue(dar.all { (_, w) -> w.latitude!! in -7.2..-6.5 && w.longitude!! in 39.0..39.6 })
    }
}
