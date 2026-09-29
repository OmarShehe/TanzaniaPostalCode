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
}
