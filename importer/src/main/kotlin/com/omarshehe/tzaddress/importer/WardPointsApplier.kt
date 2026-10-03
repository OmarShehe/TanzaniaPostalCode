package com.omarshehe.tzaddress.importer

/** Writes the result of the join into the dataset: a ward gets exactly the point its match carries, or none. */
object WardPointsApplier {

    fun apply(dataset: DatasetDto, matches: List<WardMatch>, attribution: String, version: String): DatasetDto {
        val points = matches.mapNotNull { match -> match.point?.let { match.postcode to it } }.toMap()
        return dataset.copy(
            info = dataset.info.copy(version = version, attribution = attribution),
            regions = dataset.regions.map { region ->
                region.copy(
                    districts = region.districts.map { district ->
                        district.copy(wards = district.wards.map { ward -> ward.copy(latitude = points[ward.postcode]?.latitude, longitude = points[ward.postcode]?.longitude) })
                    },
                )
            },
        )
    }
}
