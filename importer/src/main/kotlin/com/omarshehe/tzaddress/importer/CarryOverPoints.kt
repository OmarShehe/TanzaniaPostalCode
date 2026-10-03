package com.omarshehe.tzaddress.importer

/** Keeps ward positions across a re-import of the postcode list, which does not read any boundary file. */
object CarryOverPoints {

    fun merge(fresh: DatasetDto, existing: DatasetDto?): DatasetDto {
        if (existing == null) return fresh
        val points = existing.regions.flatMap { r -> r.districts.flatMap { d -> d.wards } }
            .filter { it.latitude != null && it.longitude != null }
            .associate { it.postcode to (it.latitude!! to it.longitude!!) }
        var carried = 0
        val regions = fresh.regions.map { region ->
            region.copy(
                districts = region.districts.map { district ->
                    district.copy(
                        wards = district.wards.map { ward ->
                            val point = points[ward.postcode] ?: return@map ward
                            carried++
                            ward.copy(latitude = point.first, longitude = point.second)
                        },
                    )
                },
            )
        }
        val info = if (carried > 0) fresh.info.copy(attribution = existing.info.attribution) else fresh.info
        return fresh.copy(info = info, regions = regions)
    }
}
