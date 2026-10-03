package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.AddressText
import org.locationtech.jts.index.strtree.STRtree

/**
 * Pairs each ward of the dataset with a boundary ward by name. The boundary wards carry no district, so each one is placed
 * in a district polygon through its interior point. Nothing is guessed: a name that could mean two boundary wards, or a
 * boundary ward wanted by two of our wards, gets no point.
 */
object WardPointJoin {

    private class Boundary(val nameKey: String, val districtKey: String?, val districtName: String?, val point: WardPoint)

    private val numbering = Regex("^\\d+\\.\\s*")

    /** Case, spacing, punctuation and a leading source number are ignored. */
    fun key(name: String): String = AddressText.normalize(name.replace(numbering, "")).replace(" ", "")

    fun join(dataset: DatasetDto, wardFeatures: List<BoundaryFeature>, districtFeatures: List<BoundaryFeature>): List<WardMatch> {
        val boundaries = boundaries(wardFeatures, districtFeatures)
        val byDistrictAndName = boundaries.indices.groupBy { boundaries[it].districtKey to boundaries[it].nameKey }
        val byName = boundaries.indices.groupBy { boundaries[it].nameKey }

        val ours = dataset.regions.flatMap { region -> region.districts.flatMap { district -> district.wards.map { Triple(region, district, it) } } }
            .sortedBy { (_, _, ward) -> ward.postcode }
        val regionsOfDistrictKey = dataset.regions.flatMap { r -> r.districts.map { key(it.name) to r.code } }
            .groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
        val ourNameCounts = ours.groupingBy { (_, _, ward) -> key(ward.name) }.eachCount()

        class Pick(val postcode: String, val kind: WardMatchKind, val index: Int?, val detail: String)

        val picks = ours.map { (region, district, ward) ->
            val nameKey = key(ward.name)
            val inDistrict = byDistrictAndName[key(district.name) to nameKey].orEmpty()
            val sameName = byName[nameKey].orEmpty()
            when {
                inDistrict.size == 1 -> Pick(ward.postcode, WardMatchKind.MATCHED_DISTRICT, inDistrict.single(), "")
                inDistrict.size > 1 -> Pick(ward.postcode, WardMatchKind.AMBIGUOUS, null, "${inDistrict.size} boundary wards with this name in the district")
                sameName.isEmpty() -> Pick(ward.postcode, WardMatchKind.NO_MATCH, null, "no boundary ward with this name")
                // The boundary ward is in a district that the list has only in other regions: it is elsewhere, not just spelled differently.
                sameName.size == 1 && inOtherRegionsOnly(boundaries[sameName.single()], region.code, regionsOfDistrictKey) ->
                    Pick(ward.postcode, WardMatchKind.NO_MATCH, null, "the only boundary ward with this name is in district ${boundaries[sameName.single()].districtName}, which the list has only in another region")
                sameName.size == 1 && ourNameCounts.getValue(nameKey) == 1 -> Pick(ward.postcode, WardMatchKind.MATCHED_UNIQUE_NAME, sameName.single(), "")
                else -> Pick(ward.postcode, WardMatchKind.AMBIGUOUS, null, "${sameName.size} boundary wards and ${ourNameCounts.getValue(nameKey)} postcode wards share this name; district did not settle it")
            }
        }

        val claims = picks.mapNotNull { it.index }.groupingBy { it }.eachCount()
        return picks.map { pick ->
            val index = pick.index
            when {
                index == null -> WardMatch(pick.postcode, pick.kind, null, pick.detail)
                claims.getValue(index) > 1 -> WardMatch(pick.postcode, WardMatchKind.AMBIGUOUS, null, "the same boundary ward is wanted by ${claims.getValue(index)} postcode wards")
                else -> WardMatch(pick.postcode, pick.kind, boundaries[index].point, pick.detail)
            }
        }
    }

    private fun inOtherRegionsOnly(boundary: Boundary, regionCode: String, regionsOfDistrictKey: Map<String, Set<String>>): Boolean {
        val regions = boundary.districtKey?.let { regionsOfDistrictKey[it] } ?: return false
        return regionCode !in regions
    }

    private fun boundaries(wardFeatures: List<BoundaryFeature>, districtFeatures: List<BoundaryFeature>): List<Boundary> {
        val tree = STRtree()
        districtFeatures.forEach { tree.insert(it.geometry.envelopeInternal, it) }
        return wardFeatures.map { ward ->
            val point = ward.interiorPoint()
            val location = GeoJsonBoundaries.factory.createPoint(org.locationtech.jts.geom.Coordinate(point.longitude, point.latitude))
            val district = tree.query(location.envelopeInternal).filterIsInstance<BoundaryFeature>()
                .filter { it.geometry.contains(location) }
                .minByOrNull { it.name }
            Boundary(key(ward.name), district?.let { key(it.name) }, district?.name, point)
        }
    }
}
