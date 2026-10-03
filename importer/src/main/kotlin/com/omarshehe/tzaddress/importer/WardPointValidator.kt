package com.omarshehe.tzaddress.importer

/** Checks on ward positions: a position is a pair, lies inside Tanzania, and (for a points import) covers enough wards. */
object WardPointValidator {
    /** Share of wards that must receive a position in a points import; revisit when the real rate is known. */
    const val MIN_MATCH_RATIO = 0.85

    private const val MIN_LAT = -12.0
    private const val MAX_LAT = -1.0
    private const val MIN_LON = 29.0
    private const val MAX_LON = 41.5

    private fun wards(dataset: DatasetDto) = dataset.regions.flatMap { r -> r.districts.flatMap { it.wards } }

    fun check(dataset: DatasetDto): List<Violation> = wards(dataset).mapNotNull { ward ->
        val lat = ward.latitude
        val lon = ward.longitude
        when {
            lat == null && lon == null -> null
            lat == null || lon == null ->
                Violation(ViolationKind.WARD_POINT_HALF, "Ward ${ward.postcode} '${ward.name}' has only one of latitude and longitude")
            lat !in MIN_LAT..MAX_LAT || lon !in MIN_LON..MAX_LON ->
                Violation(ViolationKind.WARD_POINT_OUT_OF_BOUNDS, "Ward ${ward.postcode} '${ward.name}' position $lat,$lon is outside Tanzania")
            else -> null
        }
    }

    fun coverage(dataset: DatasetDto, minRatio: Double): Violation? {
        val all = wards(dataset)
        val withPoint = all.count { it.latitude != null && it.longitude != null }
        val ratio = if (all.isEmpty()) 0.0 else withPoint.toDouble() / all.size
        return if (ratio < minRatio) {
            Violation(ViolationKind.WARD_POINT_COVERAGE, "Only $withPoint of ${all.size} wards (%.3f) have a position; the minimum is %.3f".format(ratio, minRatio))
        } else {
            null
        }
    }
}
