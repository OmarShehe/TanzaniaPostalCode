package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.AddressRepository
import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SqliteAddressRepositoryTest {
    private val repository: AddressRepository = runBlocking { SqliteAddressRepository.open(JdbcSQLiteDriver(), TestDb.path) }

    @AfterTest fun close() = (repository as SqliteAddressRepository).close()

    @Test fun info_isReadFromTheDatabase() = runBlocking {
        val info = repository.info()
        assertEquals("1", info.version)
        assertEquals("2012-07-30", info.sourceEdition)
    }

    @Test fun search_kivu_findsKivukoniWithFullHierarchyAndPostcode() = runBlocking {
        val matches = repository.search("kivu")
        val ward = matches.first { it.level == Level.WARD && it.label == "Kivukoni" }
        assertEquals("Dar es Salaam", ward.path.region.name)
        assertEquals("Ilala CBD", ward.path.district?.name)
        assertEquals("11101", ward.postcode)
    }

    @Test fun search_apostropheAndCase() = runBlocking {
        assertTrue(repository.search("jangombe").any { it.label == "Jang'ombe" && it.postcode == "71109" })
        assertTrue(repository.search("JANG'OMBE").any { it.label == "Jang'ombe" })
        val kariakoo = repository.search("KARIAKOO")
        assertTrue(kariakoo.isNotEmpty() && kariakoo.all { it.label.lowercase().contains("kariakoo") })
    }

    @Test fun search_ancestorNamesNarrowTheResult() = runBlocking {
        val matches = repository.search("ilala kariakoo")
        assertEquals("Kariakoo", matches.first().label)
        assertEquals(Level.WARD, matches.first().level)
        assertTrue(matches.all { it.path.district?.name == "Ilala CBD" }, "only Kariakoo places under Ilala")
    }

    @Test fun search_exactNameOutranksPrefix() = runBlocking {
        val matches = repository.search("kariakoo")
        assertEquals("Kariakoo", matches.first().label)
        assertTrue(matches.first().score > matches.first { it.label == "Kariakoo Magharibi" }.score)
    }

    @Test fun search_blankAndClamping() = runBlocking {
        assertTrue(repository.search("").isEmpty())
        assertTrue(repository.search("   ").isEmpty())
        assertEquals(1, repository.search("a", limit = 0).size)
        assertEquals(100, repository.search("a", limit = 1000).size)
    }

    @Test fun search_levelsFilter() = runBlocking {
        val matches = repository.search("kariakoo", levels = setOf(Level.WARD))
        assertTrue(matches.isNotEmpty() && matches.all { it.level == Level.WARD })
    }

    @Test fun search_everyMatchCarriesNearestWardPostcode() = runBlocking {
        val matches = repository.search("kariakoo", limit = 100)
        assertTrue(matches.filter { it.level == Level.MTAA || it.level == Level.KITONGOJI }.all { it.postcode == it.path.ward?.postcode })
        assertTrue(matches.filter { it.level == Level.WARD }.all { it.postcode == it.path.ward?.postcode })
    }

    @Test fun search_ftsSyntaxIsHarmless() = runBlocking {
        repository.search("a\" OR b* NEAR(")
        repository.search("-:*()")
        Unit
    }

    @Test fun search_regionAndDistrictLevels() = runBlocking {
        val region = repository.search("dar es salaam", levels = setOf(Level.REGION)).single()
        assertNull(region.postcode)
        assertEquals("11000", region.path.region.code)
    }

    @Test fun byPostcode_returnsFullChain() = runBlocking {
        val path = assertNotNull(repository.byPostcode("11101"))
        assertEquals("Dar es Salaam", path.region.name)
        assertEquals("Ilala CBD", path.district?.name)
        assertEquals("Kivukoni", path.ward?.name)
        assertNull(path.mtaa)
    }

    @Test fun byPostcode_unknownOrMalformed_isNull() = runBlocking {
        assertNull(repository.byPostcode("00000"))
        assertNull(repository.byPostcode("abc"))
        assertNull(repository.byPostcode(""))
        assertNull(repository.byPostcode("1110'; DROP TABLE ward;--"))
    }

    @Test fun byPrefix_returnsOnlyMatchingWards() = runBlocking {
        val wards = repository.byPrefix("111")
        assertTrue(wards.isNotEmpty())
        assertTrue(wards.all { it.ward!!.postcode.startsWith("111") })
        assertEquals(wards.map { it.ward!!.postcode }.sorted(), wards.map { it.ward!!.postcode })
        assertEquals(listOf("11101"), repository.byPrefix("11101").map { it.ward?.postcode })
        assertTrue(repository.byPrefix("11").size >= wards.size)
    }

    @Test fun byPrefix_badInput_isEmpty() = runBlocking {
        for (bad in listOf("", "1", "1234", "abc", "1a1", "1110%")) assertTrue(repository.byPrefix(bad).isEmpty(), bad)
    }

    @Test fun byPostcode_roundTripsEveryWard() = runBlocking {
        var wards = 0
        for (region in repository.regions()) for (district in repository.districts(region.code)) for (ward in repository.wards(district.code)) {
            wards++
            val path = assertNotNull(repository.byPostcode(ward.postcode), ward.postcode)
            assertEquals(ward, path.ward)
            assertEquals(district, path.district)
            assertEquals(region, path.region)
            assertTrue(repository.isValidPostcode(ward.postcode))
        }
        assertEquals(3416, wards)
    }

    @Test fun path_resolvesEveryLevel() = runBlocking {
        val mtaa = repository.mtaas("11101").first()
        val (kitongojiMtaa, kitongoji) = firstKitongoji("21000")
        assertEquals("Dar es Salaam", repository.path(Level.REGION, "11000")?.region?.name)
        assertEquals("Ilala CBD", repository.path(Level.DISTRICT, "11")?.district?.name)
        assertEquals("Kivukoni", repository.path(Level.WARD, "11101")?.ward?.name)
        assertEquals(mtaa, repository.path(Level.MTAA, mtaa.id)?.mtaa)
        val deep = assertNotNull(repository.path(Level.KITONGOJI, kitongoji.id))
        assertEquals(kitongoji, deep.kitongoji)
        assertEquals(kitongojiMtaa, deep.mtaa)
        assertEquals(kitongojiMtaa.wardPostcode, deep.ward?.postcode)
        assertEquals("21000", deep.region.code)
        assertNull(repository.path(Level.WARD, "nope"))
        assertNull(repository.path(Level.MTAA, "nope"))
    }

    @Test fun browse_isOrderedByName_andUnknownParentIsEmpty() = runBlocking {
        val regions = repository.regions()
        assertEquals(30, regions.size)
        assertEquals(regions.map { it.name.lowercase() }.sorted(), regions.map { it.name.lowercase() })
        val wards = repository.wards("11")
        assertEquals(wards.map { it.name.lowercase() }.sorted(), wards.map { it.name.lowercase() })
        assertTrue(repository.districts("nope").isEmpty())
        assertTrue(repository.wards("nope").isEmpty())
        assertTrue(repository.mtaas("nope").isEmpty())
        assertTrue(repository.kitongojis("nope").isEmpty())
    }

    @Test fun isValidPostcode_isTrueOnlyForWards() {
        assertTrue(repository.isValidPostcode("11101"))
        assertFalse(repository.isValidPostcode("11000"))
        assertFalse(repository.isValidPostcode("00000"))
        assertFalse(repository.isValidPostcode("abc"))
    }

    private suspend fun firstKitongoji(regionCode: String): Pair<Mtaa, Kitongoji> {
        for (district in repository.districts(regionCode)) for (ward in repository.wards(district.code)) for (mtaa in repository.mtaas(ward.postcode)) {
            repository.kitongojis(mtaa.id).firstOrNull()?.let { return mtaa to it }
        }
        error("no kitongoji in region $regionCode")
    }
}
