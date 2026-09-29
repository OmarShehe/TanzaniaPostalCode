package com.omarshehe.tzaddress

import com.omarshehe.tzaddress.model.DatasetInfo
import com.omarshehe.tzaddress.model.District
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import com.omarshehe.tzaddress.model.Region
import com.omarshehe.tzaddress.model.Ward

class FakeAddressRepository(
    private val info: DatasetInfo,
    private val regions: List<Region> = emptyList(),
    private val districts: List<District> = emptyList(),
    private val wards: List<Ward> = emptyList(),
    private val mtaas: List<Mtaa> = emptyList(),
    private val kitongojis: List<Kitongoji> = emptyList(),
) : AddressRepository {
    override suspend fun info() = info
    override suspend fun regions() = regions
    override suspend fun districts(regionCode: String) = districts.filter { it.regionCode == regionCode }
    override suspend fun wards(districtCode: String) = wards.filter { it.districtCode == districtCode }
    override suspend fun mtaas(wardPostcode: String) = mtaas.filter { it.wardPostcode == wardPostcode }
    override suspend fun kitongojis(mtaaId: String) = kitongojis.filter { it.mtaaId == mtaaId }

    override suspend fun search(query: String, limit: Int, levels: Set<Level>): List<AddressMatch> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return emptyList()
        val nodes = regions.map { Level.REGION to (it.code to it.name) } +
            districts.map { Level.DISTRICT to (it.code to it.name) } +
            wards.map { Level.WARD to (it.postcode to it.name) } +
            mtaas.map { Level.MTAA to (it.id to it.name) } +
            kitongojis.map { Level.KITONGOJI to (it.id to it.name) }
        return nodes
            .filter { (level, node) -> level in levels && node.second.lowercase().contains(needle) }
            .mapNotNull { (level, node) -> path(level, node.first)?.let { toMatch(it, level, node.second) } }
            .take(limit.coerceIn(1, 100))
    }

    override suspend fun byPostcode(postcode: String): AddressPath? = path(Level.WARD, postcode)

    override suspend fun byPrefix(prefix: String): List<AddressPath> =
        if (prefix.length !in setOf(2, 3, 5) || !prefix.all { it in '0'..'9' }) emptyList()
        else wards.filter { it.postcode.startsWith(prefix) }.mapNotNull { path(Level.WARD, it.postcode) }

    override suspend fun path(level: Level, id: String): AddressPath? = when (level) {
        Level.REGION -> regions.find { it.code == id }?.let { AddressPath(it) }
        Level.DISTRICT -> districts.find { it.code == id }?.let { chain(it) }
        Level.WARD -> wards.find { it.postcode == id }?.let { chain(districts.first { d -> d.code == it.districtCode }, it) }
        Level.MTAA -> mtaas.find { it.id == id }?.let { m ->
            val ward = wards.first { it.postcode == m.wardPostcode }
            chain(districts.first { it.code == ward.districtCode }, ward, m)
        }
        Level.KITONGOJI -> kitongojis.find { it.id == id }?.let { k ->
            val mtaa = mtaas.first { it.id == k.mtaaId }
            val ward = wards.first { it.postcode == mtaa.wardPostcode }
            chain(districts.first { it.code == ward.districtCode }, ward, mtaa, k)
        }
    }

    override fun isValidPostcode(postcode: String) = wards.any { it.postcode == postcode }

    private fun chain(district: District, ward: Ward? = null, mtaa: Mtaa? = null, kitongoji: Kitongoji? = null) =
        AddressPath(regions.first { it.code == district.regionCode }, district, ward, mtaa, kitongoji)

    private fun toMatch(path: AddressPath, level: Level, label: String) =
        AddressMatch(path, level, label, path.ward?.postcode, score = 1.0)
}
