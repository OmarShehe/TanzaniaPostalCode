package com.omarshehe.tzaddress

import com.omarshehe.tzaddress.model.DatasetInfo
import com.omarshehe.tzaddress.model.District
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import com.omarshehe.tzaddress.model.Region
import com.omarshehe.tzaddress.model.Ward
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
