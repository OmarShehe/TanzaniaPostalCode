package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.model.ExtraPlace
import com.omarshehe.tzaddress.model.ExtraPlaceEntry
import com.omarshehe.tzaddress.model.ExtraPlaceStatus
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class ExtraPlaceValidatorTest {
    private val base = runBlocking { SqliteAddressRepository.open(JdbcSQLiteDriver(), TestDb.path) }

    @AfterTest fun close() = base.close()

    private fun resolve(vararg extras: ExtraPlace): List<ExtraPlaceEntry> = runBlocking { ExtraPlaceValidator.resolve(base, extras.toList()) }

    private fun problems(vararg extras: ExtraPlace): List<String> =
        assertFailsWith<ExtraPlacesInvalidException> { resolve(*extras) }.problems

    private fun ward(name: String, district: String, postcode: String?, lat: Double? = null, lon: Double? = null) =
        ExtraPlace(Level.WARD, name, district, postcode, lat, lon)

    @Test fun validPlaces_resolveActive_withDerivedIds_whateverTheListOrder() {
        val entries = resolve(
            ExtraPlace(Level.KITONGOJI, "Kitongoji Kipya", "11101/soko-jipya"),
            ExtraPlace(Level.MTAA, "Soko Jipya", "11101"),
            ward("Mji Mpya", "11", "11110", -6.8, 39.28),
        )
        assertEquals(listOf("11101/soko-jipya/kitongoji-kipya", "11101/soko-jipya", "11110"), entries.map { it.id })
        assertTrue(entries.all { it.status == ExtraPlaceStatus.ACTIVE })
    }

    @Test fun everyProblemIsListedInOneException() {
        val found = problems(
            ward(" ", "11", "11110"),
            ward("No District", "99", "99001"),
            ward("Short Code", "11", "1111"),
            ward("Wrong Prefix", "11", "12110"),
            ward("No Code", "11", null),
            ExtraPlace(Level.MTAA, "Orphan", "99999"),
            ExtraPlace(Level.KITONGOJI, "Orphan Kitongoji", "11101/none"),
            ExtraPlace(Level.REGION, "Region", "x"),
            ExtraPlace(Level.MTAA, "With Postcode", "11101", postcode = "11101"),
            ward("Half Position", "11", "11111", lat = -6.8),
            ward("Outside Tanzania", "11", "11112", 10.0, 39.0),
            ExtraPlace(Level.MTAA, "Mtaa With Position", "11101", latitude = -6.8, longitude = 39.2),
        )
        assertEquals(12, found.size, found.toString())
        assertTrue(found.any { "name" in it.lowercase() })
        assertTrue(found.any { "99" in it && "district" in it.lowercase() })
        assertTrue(found.any { "1111" in it })
        assertTrue(found.any { "12110" in it && "11" in it })
        assertTrue(found.any { "99999" in it && "ward" in it.lowercase() })
        assertTrue(found.any { "11101/none" in it && "mtaa" in it.lowercase() })
    }

    @Test fun theMessageNamesEveryProblem() {
        val e = assertFailsWith<ExtraPlacesInvalidException> { resolve(ward(" ", "11", "11110"), ExtraPlace(Level.MTAA, "Orphan", "99999")) }
        assertEquals(2, e.problems.size)
        assertTrue(e.problems.all { it in e.message!! })
    }

    @Test fun duplicatesWithinTheListAreProblems() {
        val found = problems(
            ward("One", "11", "11110"), ward("Two", "11", "11110"),
            ExtraPlace(Level.MTAA, "Soko Jipya", "11101"), ExtraPlace(Level.MTAA, "SOKO  jipya", "11101"),
        )
        assertEquals(2, found.size, found.toString())
    }

    @Test fun aBuiltInDuplicateIsShadowed_byPostcodeByDistrictAndNameAndByParentAndName() {
        val entries = resolve(
            ward("Anything", "11", "11101"),
            ward("KIVUKONI", "11", "11190"),
            ExtraPlace(Level.MTAA, "SEA  view", "11101"),
            ExtraPlace(Level.MTAA, "Soko Jipya", "11101"),
        )
        assertEquals(
            listOf(ExtraPlaceStatus.SHADOWED, ExtraPlaceStatus.SHADOWED, ExtraPlaceStatus.SHADOWED, ExtraPlaceStatus.ACTIVE),
            entries.map { it.status },
        )
    }

    @Test fun aChildOfAShadowedExtraNamesTheBuiltInPlaceToUse() {
        val found = problems(ward("Kivukoni", "11", "11190"), ExtraPlace(Level.MTAA, "Child", "11190"))
        assertEquals(1, found.size, found.toString())
        assertTrue("11101" in found.single(), found.single())
    }

    @Test fun twoNamesWithTheSameSlugGetDistinctIds() {
        val entries = resolve(ExtraPlace(Level.MTAA, "Mçu", "11101"), ExtraPlace(Level.MTAA, "M U", "11101"))
        assertEquals(listOf("11101/m-u", "11101/m-u-2"), entries.map { it.id })
    }
}
