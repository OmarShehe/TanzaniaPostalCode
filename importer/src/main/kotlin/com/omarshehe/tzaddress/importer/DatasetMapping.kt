package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.AddressIds
import com.omarshehe.tzaddress.model.DatasetInfo
import com.omarshehe.tzaddress.model.District
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import com.omarshehe.tzaddress.model.Region
import com.omarshehe.tzaddress.model.Ward

/** Maps to `:core` types; ids come only from [AddressIds], the single source of id rules. */
fun DatasetDto.toCore(): CoreDataset {
    val regionList = ArrayList<Region>()
    val districtList = ArrayList<District>()
    val wardList = ArrayList<Ward>()
    val mtaaList = ArrayList<Mtaa>()
    val kitongojiList = ArrayList<Kitongoji>()
    for (region in regions) {
        regionList += Region(region.code, region.name)
        for (district in region.districts) {
            districtList += District(district.code, district.name, region.code)
            for (ward in district.wards) {
                wardList += Ward(ward.postcode, ward.name, district.code, ward.latitude, ward.longitude)
                val mtaaIds = AddressIds.mtaaIds(ward.postcode, ward.mtaas.map { it.name })
                ward.mtaas.forEachIndexed { i, mtaa ->
                    mtaaList += Mtaa(mtaaIds[i], mtaa.name, ward.postcode)
                    val kitongojiIds = AddressIds.kitongojiIds(mtaaIds[i], mtaa.kitongojis)
                    mtaa.kitongojis.forEachIndexed { j, name -> kitongojiList += Kitongoji(kitongojiIds[j], name, mtaaIds[i]) }
                }
            }
        }
    }
    return CoreDataset(DatasetInfo(info.version, info.sourceEdition, info.generatedAt, info.attribution), regionList, districtList, wardList, mtaaList, kitongojiList)
}
