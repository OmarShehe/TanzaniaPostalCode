package com.omarshehe.tzaddress.importer

/** Keeps ward positions across a re-import of the postcode list, which does not read any boundary file. */
object CarryOverPoints {

    /** A postcode can be reused for another ward in a later edition, so a position follows the postcode only while the ward's name is the same. */
    private fun sameWard(a: String, b: String) = key(a) == key(b)

    private fun key(name: String) = name.filter { it.isLetterOrDigit() }.lowercase()

    fun merge(fresh: DatasetDto, existing: DatasetDto?): DatasetDto {
        if (existing == null) return fresh
        val points = existing.regions.flatMap { r -> r.districts.flatMap { d -> d.wards } }
            .filter { it.latitude != null && it.longitude != null }
            .associate { it.postcode to Triple(it.name, it.latitude!!, it.longitude!!) }
        var carried = 0
        val regions = fresh.regions.map { region ->
            region.copy(
                districts = region.districts.map { district ->
                    district.copy(
                        wards = district.wards.map { ward ->
                            val point = points[ward.postcode]?.takeIf { sameWard(it.first, ward.name) } ?: return@map ward
                            carried++
                            ward.copy(latitude = point.second, longitude = point.third)
                        },
                    )
                },
            )
        }
        val info = if (carried > 0) fresh.info.copy(attribution = existing.info.attribution) else fresh.info
        return fresh.copy(info = info, regions = regions)
    }
}
