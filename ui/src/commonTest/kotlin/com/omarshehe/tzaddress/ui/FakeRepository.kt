package com.omarshehe.tzaddress.ui

import com.omarshehe.tzaddress.AddressIds
import com.omarshehe.tzaddress.AddressMatch
import com.omarshehe.tzaddress.AddressPath
import com.omarshehe.tzaddress.AddressRepository
import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.model.DatasetInfo
import com.omarshehe.tzaddress.model.District
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import com.omarshehe.tzaddress.model.Region
import com.omarshehe.tzaddress.model.Ward

/** In-memory repository for UI tests: a small Dar es Salaam / Tanga hierarchy plus a programmable search. */
class FakeRepository(
    var searchHandler: suspend (String) -> List<AddressMatch> = { emptyList() },
    var failBrowse: Boolean = false,
) : AddressRepository {
    val searchQueries = mutableListOf<String>()

    val dar = Region("11000", "Dar es Salaam")
    val tanga = Region("21000", "Tanga")
    val ilala = District("11", "Ilala CBD", dar.code)
    val temeke = District("12", "Temeke", dar.code)
    val kivukoni = Ward("11101", "Kivukoni", ilala.code)
    val kariakoo = Ward("11106", "Kariakoo", ilala.code) // no mtaa: "None listed"
    val kivukoniMtaa = Mtaa(AddressIds.mtaaIds(kivukoni.postcode, listOf("Kivukoni")).single(), "Kivukoni", kivukoni.postcode)
    val seaView = Kitongoji(AddressIds.kitongojiIds(kivukoniMtaa.id, listOf("Sea View")).single(), "Sea View", kivukoniMtaa.id) // sole kitongoji
    val ferryMtaa = Mtaa(AddressIds.mtaaIds(kivukoni.postcode, listOf("Kivukoni", "Ferry"))[1], "Ferry", kivukoni.postcode) // no kitongoji

    private val regions = listOf(dar, tanga)
    private val districts = listOf(ilala, temeke)
    private val wards = listOf(kivukoni, kariakoo)
    private val mtaas = listOf(kivukoniMtaa, ferryMtaa)
    private val kitongojis = listOf(seaView)

    override suspend fun info() = DatasetInfo("1", "test", "2026-01-01T00:00:00Z")
    override suspend fun regions() = if (failBrowse) error("boom") else regions
    override suspend fun districts(regionCode: String) = if (failBrowse) error("boom") else districts.filter { it.regionCode == regionCode }
    override suspend fun wards(districtCode: String) = if (failBrowse) error("boom") else wards.filter { it.districtCode == districtCode }
    override suspend fun mtaas(wardPostcode: String) = if (failBrowse) error("boom") else mtaas.filter { it.wardPostcode == wardPostcode }
    override suspend fun kitongojis(mtaaId: String) = if (failBrowse) error("boom") else kitongojis.filter { it.mtaaId == mtaaId }

    override suspend fun search(query: String, limit: Int, levels: Set<Level>): List<AddressMatch> {
        searchQueries += query
        return searchHandler(query)
    }

    override suspend fun byPostcode(postcode: String) = path(Level.WARD, postcode)
    override suspend fun byPrefix(prefix: String) = wards.filter { it.postcode.startsWith(prefix) }.mapNotNull { path(Level.WARD, it.postcode) }
    override fun isValidPostcode(postcode: String) = wards.any { it.postcode == postcode }

    override suspend fun path(level: Level, id: String): AddressPath? = when (level) {
        Level.REGION -> regions.find { it.code == id }?.let { AddressPath(it) }
        Level.DISTRICT -> districts.find { it.code == id }?.let { AddressPath(region(it), it) }
        Level.WARD -> wards.find { it.postcode == id }?.let { w -> district(w).let { AddressPath(region(it), it, w) } }
        Level.MTAA -> mtaas.find { it.id == id }?.let { m -> ward(m).let { w -> district(w).let { AddressPath(region(it), it, w, m) } } }
        Level.KITONGOJI -> kitongojis.find { it.id == id }?.let { k ->
            val m = mtaas.first { it.id == k.mtaaId }
            val w = ward(m)
            district(w).let { AddressPath(region(it), it, w, m, k) }
        }
    }

    private fun region(d: District) = regions.first { it.code == d.regionCode }
    private fun district(w: Ward) = districts.first { it.code == w.districtCode }
    private fun ward(m: Mtaa) = wards.first { it.postcode == m.wardPostcode }

    fun match(level: Level, id: String, label: String): AddressMatch {
        val path = checkNotNull(kotlinx.coroutines.runBlocking { path(level, id) })
        return AddressMatch(path, level, label, path.ward?.postcode, 1.0)
    }
}
