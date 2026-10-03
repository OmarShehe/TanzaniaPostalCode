package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.AddressText
import java.util.Locale
import org.locationtech.jts.index.strtree.STRtree

/**
 * Pairs each ward of the dataset with a boundary ward by name. The boundary wards carry no district, so each one is placed
 * in a district polygon through its interior point. Nothing is guessed: a name that could mean two boundary wards, or a
 * boundary ward wanted by two of our wards, gets no point. A ward with no exact name may still match a spelling variant in
 * its own district, but only when that boundary ward is the single clear best candidate in both directions.
 */
object WardPointJoin {

    /** The least [NameSimilarity] a spelling variant needs. */
    const val SIMILAR_NAME_THRESHOLD = 0.85

    /** The best candidate must beat the runner-up by more than this, or the choice is not clear. */
    private const val CLEAR_MARGIN = 0.03

    private class Boundary(val name: String, val nameKey: String, val districtKey: String?, val districtName: String?, val point: WardPoint)

    private val numbering = Regex("^\\d+\\.\\s*")
    private val splitSuffix = Regex("\\s+[A-Za-z0-9]{1,2}$")

    private class OurWard(val postcode: String, val districtKey: String, val nameKey: String, val splitParentKey: String?)

    /** "Matale A" and "Kitama 1" read as later splits of "Matale" and "Kitama": the parent's polygon is not their own, so it is not a spelling variant. */
    private fun splitParentKey(name: String): String? =
        name.replace(numbering, "").takeIf { splitSuffix.containsMatchIn(it) }?.let { key(it.replace(splitSuffix, "")) }

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

        val similar = similarNames(ours.map { (_, district, ward) -> OurWard(ward.postcode, key(district.name), key(ward.name), splitParentKey(ward.name)) }, picks.map { it.kind }, picks.mapNotNull { it.index }.toSet(), boundaries)
        val claims = picks.mapNotNull { it.index }.groupingBy { it }.eachCount()
        return picks.map { pick ->
            similar[pick.postcode]?.let { (index, score) ->
                return@map WardMatch(pick.postcode, WardMatchKind.MATCHED_SIMILAR_NAME, boundaries[index].point, "similar to boundary ward '${boundaries[index].name}' (${"%.2f".format(Locale.ROOT, score)})")
            }
            val index = pick.index
            when {
                index == null -> WardMatch(pick.postcode, pick.kind, null, pick.detail)
                claims.getValue(index) > 1 -> WardMatch(pick.postcode, WardMatchKind.AMBIGUOUS, null, "the same boundary ward is wanted by ${claims.getValue(index)} postcode wards")
                else -> WardMatch(pick.postcode, pick.kind, boundaries[index].point, pick.detail)
            }
        }
    }

    /** For wards without an exact name: the one boundary ward of the same district that is clearly the best match for it, and it for the ward. */
    private fun similarNames(ours: List<OurWard>, kinds: List<WardMatchKind>, taken: Set<Int>, boundaries: List<Boundary>): Map<String, Pair<Int, Double>> {
        val byDistrict = boundaries.indices.filter { it !in taken }.groupBy { boundaries[it].districtKey }
        val scores = ours.indices.filter { kinds[it] == WardMatchKind.NO_MATCH }.associateWith { i ->
            byDistrict[ours[i].districtKey].orEmpty()
                .filter { b -> boundaries[b].nameKey != ours[i].splitParentKey }
                .map { b -> b to NameSimilarity.score(ours[i].nameKey, boundaries[b].nameKey) }
                .filter { it.second >= SIMILAR_NAME_THRESHOLD }
        }
        val bestOfWard = scores.mapValues { (_, list) -> clearBest(list) }
        val wardsOfBoundary = scores.flatMap { (i, list) -> list.map { (b, score) -> b to (i to score) } }.groupBy({ it.first }, { it.second })
        val result = HashMap<String, Pair<Int, Double>>()
        for ((i, best) in bestOfWard) {
            val (b, score) = best ?: continue
            val clearWard = clearBest(wardsOfBoundary.getValue(b).map { it.first to it.second })
            if (clearWard?.first == i) result[ours[i].postcode] = b to score
        }
        return result
    }

    private fun clearBest(candidates: List<Pair<Int, Double>>): Pair<Int, Double>? {
        val sorted = candidates.sortedByDescending { it.second }
        val best = sorted.firstOrNull() ?: return null
        return best.takeIf { sorted.size == 1 || best.second - sorted[1].second > CLEAR_MARGIN }
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
            Boundary(ward.name, key(ward.name), district?.let { key(it.name) }, district?.name, point)
        }
    }
}
