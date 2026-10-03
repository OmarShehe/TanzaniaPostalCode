package com.omarshehe.tzaddress.importer

data class WardRef(val regionCode: String, val postcode: String, val name: String)
data class RecodedWard(val regionCode: String, val oldPostcode: String, val newPostcode: String, val oldName: String, val newName: String)
data class RenamedWard(val regionCode: String, val postcode: String, val oldName: String, val newName: String)
data class RegionRef(val code: String, val name: String)
data class DistrictRef(val code: String, val name: String, val regionCode: String)

/** What changed between two editions of the dataset, matched by ward postcode and the new list's old-postcode column. */
class EditionChanges(
    val added: List<WardRef>,
    val removed: List<WardRef>,
    val recoded: List<RecodedWard>,
    val renamed: List<RenamedWard>,
    val addedRegions: List<RegionRef>,
    val removedRegions: List<RegionRef>,
    val addedDistricts: List<DistrictRef>,
    val removedDistricts: List<DistrictRef>,
    val regionNames: Map<String, String>,
) {
    companion object {
        /** [oldPostcodes] maps a ward's new postcode to the previous postcode given in the new list. */
        fun compare(previous: DatasetDto, current: DatasetDto, oldPostcodes: Map<String, String>): EditionChanges {
            val after = wards(current)
            val before = wards(previous).associateBy { it.postcode }
            val recodedFrom = after.mapNotNull { w -> oldPostcodes[w.postcode]?.takeIf { it != w.postcode && it in before }?.let { w.postcode to it } }.toMap()
            val claimedOld = recodedFrom.values.toSet()

            val added = ArrayList<WardRef>()
            val recoded = ArrayList<RecodedWard>()
            val renamed = ArrayList<RenamedWard>()
            val matched = HashSet<String>()
            for (ward in after) {
                val old = recodedFrom[ward.postcode]
                val same = before[ward.postcode]
                when {
                    old != null -> {
                        matched += old
                        recoded += RecodedWard(ward.regionCode, old, ward.postcode, before.getValue(old).name, ward.name)
                    }
                    same != null && ward.postcode !in claimedOld -> {
                        matched += ward.postcode
                        if (!same.name.equals(ward.name, ignoreCase = true)) renamed += RenamedWard(ward.regionCode, ward.postcode, same.name, ward.name)
                    }
                    else -> added += ward
                }
            }
            val removed = before.values.filter { it.postcode !in matched }

            val regionsBefore = previous.regions.associate { it.code to RegionRef(it.code, it.name) }
            val regionsAfter = current.regions.associate { it.code to RegionRef(it.code, it.name) }
            val districtsBefore = districts(previous)
            val districtsAfter = districts(current)
            return EditionChanges(
                added = added,
                removed = removed,
                recoded = recoded,
                renamed = renamed,
                addedRegions = (regionsAfter - regionsBefore.keys).values.toList(),
                removedRegions = (regionsBefore - regionsAfter.keys).values.toList(),
                addedDistricts = (districtsAfter - districtsBefore.keys).values.toList(),
                removedDistricts = (districtsBefore - districtsAfter.keys).values.toList(),
                regionNames = (regionsBefore + regionsAfter).mapValues { it.value.name },
            )
        }

        /** Previous postcodes that more than one new ward claims, with the wards that claim them. */
        fun duplicateOldPostcodes(oldPostcodes: Map<String, String>): Map<String, List<String>> =
            oldPostcodes.entries.groupBy({ it.value }, { it.key }).filterValues { it.size > 1 }.mapValues { it.value.sorted() }

        private fun wards(dataset: DatasetDto): List<WardRef> =
            dataset.regions.flatMap { r -> r.districts.flatMap { d -> d.wards.map { WardRef(r.code, it.postcode, it.name) } } }

        private fun districts(dataset: DatasetDto): Map<String, DistrictRef> =
            dataset.regions.flatMap { r -> r.districts.map { DistrictRef(it.code, it.name, r.code) } }.associateBy { it.code }
    }
}
