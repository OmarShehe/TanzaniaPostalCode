package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.importer.DatasetFixtures.valid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatasetWriterTest {

    @Test fun sameDataset_writesIdenticalText() {
        assertEquals(DatasetWriter.toJson(valid()), DatasetWriter.toJson(valid()))
    }

    @Test fun regionsAndDistrictsAndWards_areSortedByCode() {
        val json = DatasetWriter.toJson(valid())
        assertTrue(json.indexOf("\"11000\"") < json.indexOf("\"53000\""))
        assertTrue(json.indexOf("\"53101\"") < json.indexOf("\"53102\""))
    }

    @Test fun mtaasAndKitongojis_keepSourceOrder() {
        val ds = DatasetFixtures.dataset(
            listOf(
                RegionDto(
                    "53000", "Mbeya",
                    listOf(DistrictDto("531", "A", listOf(WardDto("53101", "W", listOf(MtaaDto("Zed", listOf("b", "a")), MtaaDto("Alpha", emptyList())))))),
                ),
            ),
        )
        val json = DatasetWriter.toJson(ds)
        assertTrue(json.indexOf("Zed") < json.indexOf("Alpha"))
        assertTrue(json.indexOf("\"b\"") < json.indexOf("\"a\""))
    }

    @Test fun output_usesUnixNewlinesAndEndsWithOne() {
        val json = DatasetWriter.toJson(valid())
        assertTrue('\r' !in json)
        assertTrue(json.endsWith("\n"))
    }

    @Test fun roundTrip_parsesBackToTheSameDataset() {
        val original = valid()
        val parsed = DatasetWriter.fromJson(DatasetWriter.toJson(original))
        assertEquals(DatasetWriter.sorted(original), parsed)
    }

    @Test fun coreMapping_resolvesParentsAndUsesAddressIds() {
        val ds = DatasetFixtures.dataset(
            listOf(RegionDto("53000", "Mbeya", listOf(DistrictDto("531", "A", listOf(WardDto("53101", "W", listOf(MtaaDto("Rubumba", listOf("K")), MtaaDto("Rubumba", emptyList())))))))),
        )
        val core = ds.toCore()
        assertEquals(listOf("53101/rubumba", "53101/rubumba-2"), core.mtaas.map { it.id })
        assertEquals("53101/rubumba/k", core.kitongojis.single().id)
        assertEquals("531", core.wards.single().districtCode)
        assertEquals("53000", core.districts.single().regionCode)
    }
}
