package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.model.ExtraPlace
import com.omarshehe.tzaddress.model.ExtraPlaceStatus
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class LayeredAddressStoreTest {
    private val base = runBlocking { SqliteAddressRepository.open(JdbcSQLiteDriver(), TestDb.path) }
    private val plain = runBlocking { SqliteAddressRepository.open(JdbcSQLiteDriver(), TestDb.path) }

    private val extras = listOf(
        ExtraPlace(Level.WARD, "Mji Mpya", "11", "11110", -6.8, 39.28),
        ExtraPlace(Level.MTAA, "Soko Jipya", "11101"),
        ExtraPlace(Level.KITONGOJI, "Kitongoji Kipya", "11101/soko-jipya"),
        ExtraPlace(Level.MTAA, "Mtaa wa Majaribio", "11110"),
        ExtraPlace(Level.WARD, "Kata Mpya", "12", "12128"),
        ExtraPlace(Level.WARD, "Kivukoni Mpya", "11", "11111"),
        ExtraPlace(Level.WARD, "Kivukoni", "11", "11190"),
    )

    private val layered = runBlocking { LayeredAddressStore.create(base, extras) }

    @AfterTest fun close() {
        layered.close()
        plain.close()
    }

    @Test fun anExtraMtaaIsInTheBrowseListInNameOrder() = runBlocking {
        assertEquals(listOf("Kivukoni", "Sea View", "Soko Jipya"), layered.mtaas("11101").map { it.name })
        assertEquals(listOf("Kivukoni", "Sea View"), plain.mtaas("11101").map { it.name })
    }

    @Test fun anExtraKitongojiUnderAnExtraMtaaIsListed() = runBlocking {
        assertEquals(listOf("Kitongoji Kipya"), layered.kitongojis("11101/soko-jipya").map { it.name })
    }

    @Test fun extraWardsAreInTheirDistrictsAndLookups() = runBlocking {
        assertTrue("Mji Mpya" in layered.wards("11").map { it.name })
        assertEquals(listOf("Kata Mpya"), layered.wards("12").map { it.name }.filter { it == "Kata Mpya" })
        val ward = assertNotNull(layered.byPostcode("11110")).ward
        assertEquals(-6.8, ward?.latitude)
        assertEquals(39.28, ward?.longitude)
        assertNull(plain.byPostcode("11110"))
        assertTrue(layered.isValidPostcode("12128"))
        assertTrue(!plain.isValidPostcode("12128"))
    }

    @Test fun byPrefixIncludesExtraWardsInPostcodeOrder() = runBlocking {
        val codes = layered.byPrefix("111").map { it.ward!!.postcode }
        assertEquals(codes.sorted(), codes)
        assertTrue("11110" in codes && "11111" in codes && "11101" in codes)
        assertEquals(listOf("12128"), layered.byPrefix("12128").map { it.ward!!.postcode })
    }

    @Test fun browseOrderIsByNameThenIdAndStable() = runBlocking {
        val first = layered.wards("11")
        assertEquals(first, layered.wards("11"))
        assertEquals(first.map { it.name.lowercase() to it.postcode }.sortedWith(compareBy({ it.first }, { it.second })), first.map { it.name.lowercase() to it.postcode })
    }

    @Test fun pathsCarryEveryAncestor_forEachLevel() = runBlocking {
        val mtaa = assertNotNull(layered.path(Level.MTAA, "11101/soko-jipya"))
        assertEquals(listOf("Dar es Salaam", "Ilala CBD", "Kivukoni", "Soko Jipya"), listOf(mtaa.region.name, mtaa.district?.name, mtaa.ward?.name, mtaa.mtaa?.name))
        val kitongoji = assertNotNull(layered.path(Level.KITONGOJI, "11101/soko-jipya/kitongoji-kipya"))
        assertEquals(listOf("Ilala CBD", "Kivukoni", "Soko Jipya", "Kitongoji Kipya"), listOf(kitongoji.district?.name, kitongoji.ward?.name, kitongoji.mtaa?.name, kitongoji.kitongoji?.name))
        val underExtraWard = assertNotNull(layered.path(Level.MTAA, "11110/mtaa-wa-majaribio"))
        assertEquals(listOf("Ilala CBD", "Mji Mpya"), listOf(underExtraWard.district?.name, underExtraWard.ward?.name))
        assertEquals(base.path(Level.WARD, "11101"), layered.path(Level.WARD, "11101"))
        assertEquals(base.path(Level.DISTRICT, "11"), layered.path(Level.DISTRICT, "11"))
    }

    @Test fun aShadowedExtraIsNotReturnedAndIsReported() = runBlocking {
        assertEquals(1, layered.wards("11").count { it.name == "Kivukoni" })
        assertNull(layered.path(Level.WARD, "11190"))
        val statuses = layered.extraPlaceStatuses().associate { it.place.name to it.status }
        assertEquals(ExtraPlaceStatus.SHADOWED, statuses["Kivukoni"])
        assertEquals(ExtraPlaceStatus.ACTIVE, statuses["Soko Jipya"])
        assertEquals("11101/soko-jipya", layered.extraPlaceStatuses().first { it.place.name == "Soko Jipya" }.id)
    }

    @Test fun searchFindsAnExtraByItsNameWithTheSameRankingAsABuiltInPlace() = runBlocking {
        val exact = layered.search("soko jipya").first()
        assertEquals(Level.MTAA, exact.level)
        assertEquals("Soko Jipya", exact.label)
        assertEquals("11101", exact.postcode)
        assertEquals(4.0, exact.score)
        val prefix = layered.search("kivukoni", limit = 100).first { it.label == "Kivukoni Mpya" }
        assertEquals(3.0, prefix.score)
    }

    @Test fun searchFindsAnExtraByAnAncestorName_afterTheBetterMatches() = runBlocking {
        val matches = layered.search("kivukoni", limit = 100)
        val child = matches.indexOfFirst { it.label == "Soko Jipya" }
        assertTrue(child > 0)
        assertEquals(0.0, matches[child].score)
        assertTrue(matches.take(child).all { it.score >= 0.0 })
    }

    @Test fun searchRespectsLevelsAndTheLimitAfterTheMerge() = runBlocking {
        assertTrue(layered.search("mji mpya", levels = setOf(Level.MTAA)).none { it.level == Level.WARD })
        assertEquals(5, layered.search("a", limit = 5).size)
        assertTrue(layered.search("  ").isEmpty())
        assertEquals(layered.search("kivu", limit = 30), layered.search("kivu", limit = 30))
    }

    @Test fun withoutExtrasTheResultsAreTheSameAsTheBaseStore() = runBlocking {
        val empty = LayeredAddressStore.create(plain, emptyList())
        assertEquals(base.search("kariakoo"), empty.search("kariakoo"))
        assertEquals(base.wards("11"), empty.wards("11"))
        assertEquals(base.byPrefix("111"), empty.byPrefix("111"))
        assertEquals(base.info(), empty.info())
        assertEquals(base.regions(), empty.regions())
    }

    @Test fun closingClosesTheBaseStore() {
        layered.close()
        assertFailsWith<IllegalStateException> { runBlocking { base.regions() } }
    }

    @Test fun aFailedCreationClosesTheBaseStore() {
        val victim = runBlocking { SqliteAddressRepository.open(JdbcSQLiteDriver(), TestDb.path) }
        assertFailsWith<ExtraPlacesInvalidException> { runBlocking { LayeredAddressStore.create(victim, listOf(ExtraPlace(Level.MTAA, " ", "99999"))) } }
        assertFailsWith<IllegalStateException> { runBlocking { victim.regions() } }
    }

    @Test fun anExtraKitongojiUnderABuiltInMtaaHasTheBuiltInAncestors() = runBlocking {
        var mtaaId: String? = null
        search@ for (region in plain.regions()) for (district in plain.districts(region.code)) for (ward in plain.wards(district.code)) for (mtaa in plain.mtaas(ward.postcode)) {
            if (plain.kitongojis(mtaa.id).isNotEmpty()) {
                mtaaId = mtaa.id
                break@search
            }
        }
        val parent = assertNotNull(mtaaId)
        val store = LayeredAddressStore.create(runBlocking { SqliteAddressRepository.open(JdbcSQLiteDriver(), TestDb.path) }, listOf(ExtraPlace(Level.KITONGOJI, "Kitongoji Kipya Sana", parent)))
        val builtIn = plain.kitongojis(parent).map { it.name }
        assertEquals((builtIn + "Kitongoji Kipya Sana").sortedBy { it.lowercase() }, store.kitongojis(parent).map { it.name })
        val path = assertNotNull(store.path(Level.KITONGOJI, "$parent/kitongoji-kipya-sana"))
        assertEquals(plain.path(Level.MTAA, parent), path.copy(kitongoji = null))
        assertEquals("Kitongoji Kipya Sana", store.search("kitongoji kipya sana").first().label)
        store.close()
    }
}
