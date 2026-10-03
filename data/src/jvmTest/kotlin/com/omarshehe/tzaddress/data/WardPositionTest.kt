package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import com.omarshehe.tzaddress.model.Ward
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class WardPositionTest {
    private val repository = runBlocking { SqliteAddressRepository.open(JdbcSQLiteDriver(), TestDb.path) }

    @AfterTest fun close() = repository.close()

    @Test fun matchedWardCarriesItsPosition() = runBlocking {
        val ward = assertNotNull(repository.byPostcode("11101")?.ward)
        assertEquals(-6.80919, ward.latitude)
        assertEquals(39.29169, ward.longitude)
    }

    @Test fun unmatchedWardHasNoPositionAndNoError() = runBlocking {
        val ward = assertNotNull(repository.byPostcode("15122")?.ward)
        assertNull(ward.latitude)
        assertNull(ward.longitude)
    }

    @Test fun browseSearchAndPathsCarryThePositionToo() = runBlocking {
        assertEquals(-6.80919, repository.wards("11").first { it.postcode == "11101" }.latitude)
        val match = repository.search("kivukoni").first { it.level == Level.WARD && it.postcode == "11101" }
        assertEquals(39.29169, match.path.ward?.longitude)
        assertEquals(-6.80919, repository.byPrefix("11101").single().ward?.latitude)
    }

    @Test fun deeperLevelsStillReadTheirOwnColumnsAfterTheWardColumnsGrew() = runBlocking {
        // Any positioned ward whose mtaa has a kitongoji, so every level of the path is filled.
        var found: Triple<Ward, Mtaa, Kitongoji>? = null
        search@ for (region in repository.regions()) for (district in repository.districts(region.code)) for (w in repository.wards(district.code)) {
            if (w.latitude == null) continue
            for (m in repository.mtaas(w.postcode)) {
                val k = repository.kitongojis(m.id).firstOrNull() ?: continue
                found = Triple(w, m, k)
                break@search
            }
        }
        val (ward, mtaa, kitongoji) = assertNotNull(found)
        val viaKitongoji = assertNotNull(repository.path(Level.KITONGOJI, kitongoji.id))
        assertEquals(kitongoji, viaKitongoji.kitongoji)
        assertEquals(mtaa, viaKitongoji.mtaa)
        assertEquals(ward, viaKitongoji.ward)
        assertEquals(mtaa, repository.path(Level.MTAA, mtaa.id)?.mtaa)
    }

    @Test fun infoCarriesTheVersionAndTheAttribution() = runBlocking {
        val info = repository.info()
        assertEquals("2", info.version)
        assertContains(info.attribution, "© OpenStreetMap contributors")
        assertContains(info.attribution, "ODbL")
    }
}
