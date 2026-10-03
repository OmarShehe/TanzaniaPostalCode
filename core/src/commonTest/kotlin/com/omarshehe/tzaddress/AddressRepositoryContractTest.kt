package com.omarshehe.tzaddress

import com.omarshehe.tzaddress.model.DatasetInfo
import com.omarshehe.tzaddress.model.District
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import com.omarshehe.tzaddress.model.Region
import com.omarshehe.tzaddress.model.Ward
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AddressRepositoryContractTest {

    private val info = DatasetInfo(version = "1", sourceEdition = "test", generatedAt = "2026-01-01T00:00:00Z")
    private val mtaaId = AddressIds.mtaaIds("11101", listOf("Kivukoni")).single()
    private val kitongojiId = AddressIds.kitongojiIds(mtaaId, listOf("Sea View")).single()

    private val repository: AddressRepository = FakeAddressRepository(
        info = info,
        regions = listOf(Region("11000", "Dar es Salaam")),
        districts = listOf(District("11", "Ilala CBD", "11000")),
        wards = listOf(Ward("11101", "Kivukoni", "11")),
        mtaas = listOf(Mtaa(mtaaId, "Kivukoni", "11101")),
        kitongojis = listOf(Kitongoji(kitongojiId, "Sea View", mtaaId)),
    )

    @Test
    fun info_returnsDatasetInfo() = runTest {
        assertEquals(info, repository.info())
    }

    @Test
    fun browse_resolvesTheWholeChain() = runTest {
        val region = repository.regions().single()
        val district = repository.districts(region.code).single()
        val ward = repository.wards(district.code).single()
        val mtaa = repository.mtaas(ward.postcode).single()
        val kitongoji = repository.kitongojis(mtaa.id).single()
        assertEquals("Sea View", kitongoji.name)
    }

    @Test
    fun browse_unknownParent_isEmpty() = runTest {
        assertTrue(repository.districts("nope").isEmpty())
        assertTrue(repository.wards("nope").isEmpty())
        assertTrue(repository.mtaas("nope").isEmpty())
        assertTrue(repository.kitongojis("nope").isEmpty())
    }

    @Test
    fun byPostcode_returnsChain_orNullForUnknownOrMalformed() = runTest {
        val path = repository.byPostcode("11101")
        assertEquals("Dar es Salaam", path?.region?.name)
        assertEquals("Ilala CBD", path?.district?.name)
        assertEquals("Kivukoni", path?.ward?.name)
        assertNull(path?.mtaa)
        assertNull(repository.byPostcode("00000"))
        assertNull(repository.byPostcode("abc"))
        assertNull(repository.byPostcode(""))
    }

    @Test
    fun byPrefix_matchesWardsOnly_andIgnoresBadInput() = runTest {
        assertEquals(listOf("11101"), repository.byPrefix("111").map { it.ward?.postcode })
        assertTrue(repository.byPrefix("999").isEmpty())
        assertTrue(repository.byPrefix("1x").isEmpty())
        assertTrue(repository.byPrefix("").isEmpty())
    }

    @Test
    fun path_resolvesEveryLevel() = runTest {
        assertEquals("Dar es Salaam", repository.path(Level.REGION, "11000")?.region?.name)
        assertEquals("Ilala CBD", repository.path(Level.DISTRICT, "11")?.district?.name)
        assertEquals("Kivukoni", repository.path(Level.WARD, "11101")?.ward?.name)
        assertEquals("Kivukoni", repository.path(Level.MTAA, mtaaId)?.mtaa?.name)
        val kitongoji = repository.path(Level.KITONGOJI, kitongojiId)
        assertEquals("Sea View", kitongoji?.kitongoji?.name)
        assertEquals("11101", kitongoji?.ward?.postcode)
        assertNull(repository.path(Level.WARD, "nope"))
    }

    @Test
    fun isValidPostcode_isTrueOnlyForKnownWards() {
        assertTrue(repository.isValidPostcode("11101"))
        assertFalse(repository.isValidPostcode("11000"))
        assertFalse(repository.isValidPostcode("abc"))
    }

    @Test
    fun search_blankQuery_isEmpty() = runTest {
        assertTrue(repository.search("").isEmpty())
        assertTrue(repository.search("   ").isEmpty())
    }

    @Test
    fun search_matchCarriesNearestAncestorPostcode() = runTest {
        val match = repository.search("sea").single()
        assertEquals(Level.KITONGOJI, match.level)
        assertEquals("11101", match.postcode)
        assertEquals("Sea View", match.label)
    }

    @Test
    fun level_all_listsEveryLevelFromRegionDown() {
        assertEquals(
            listOf(Level.REGION, Level.DISTRICT, Level.WARD, Level.MTAA, Level.KITONGOJI),
            Level.all.toList(),
        )
    }
}
