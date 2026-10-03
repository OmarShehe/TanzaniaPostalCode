package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.AddressMatch
import com.omarshehe.tzaddress.AddressPath
import com.omarshehe.tzaddress.AddressText
import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.model.DatasetInfo
import com.omarshehe.tzaddress.model.District
import com.omarshehe.tzaddress.model.ExtraPlace
import com.omarshehe.tzaddress.model.ExtraPlaceEntry
import com.omarshehe.tzaddress.model.ExtraPlaceStatus
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import com.omarshehe.tzaddress.model.Region
import com.omarshehe.tzaddress.model.Ward

/**
 * The bundled store plus an app's extra places, held in memory. Built for tens or hundreds of places. Browse, lookup and search
 * return both, ordered and ranked by the same rules; the bundled database is only read. [close] closes [base].
 */
internal class LayeredAddressStore private constructor(
    private val base: AddressStore,
    private val entries: List<ExtraPlaceEntry>,
) : AddressStore {

    private val active = entries.filter { it.status == ExtraPlaceStatus.ACTIVE }
    private val wards: List<Ward> = active.filter { it.place.level == Level.WARD }
        .map { Ward(it.id, it.place.name.trim(), it.place.parentId, it.place.latitude, it.place.longitude) }
    private val mtaas: List<Mtaa> = active.filter { it.place.level == Level.MTAA }.map { Mtaa(it.id, it.place.name.trim(), it.place.parentId) }
    private val kitongojis: List<Kitongoji> = active.filter { it.place.level == Level.KITONGOJI }.map { Kitongoji(it.id, it.place.name.trim(), it.place.parentId) }
    private val wardsByPostcode = wards.associateBy { it.postcode }
    private val mtaasById = mtaas.associateBy { it.id }
    private val kitongojisById = kitongojis.associateBy { it.id }

    /** The path of every active extra place, resolved once at creation: the extras and the bundled data never change while the store is open. */
    private val paths = HashMap<Pair<Level, String>, AddressPath>()

    override fun extraPlaceStatuses(): List<ExtraPlaceEntry> = entries

    override suspend fun info(): DatasetInfo = base.info()
    override suspend fun regions(): List<Region> = base.regions()
    override suspend fun districts(regionCode: String): List<District> = base.districts(regionCode)

    override suspend fun wards(districtCode: String): List<Ward> =
        (base.wards(districtCode) + wards.filter { it.districtCode == districtCode }).sortedWith(compareBy({ it.name.lowercase() }, { it.postcode }))

    override suspend fun mtaas(wardPostcode: String): List<Mtaa> =
        (base.mtaas(wardPostcode) + mtaas.filter { it.wardPostcode == wardPostcode }).sortedWith(compareBy({ it.name.lowercase() }, { it.id }))

    override suspend fun kitongojis(mtaaId: String): List<Kitongoji> =
        (base.kitongojis(mtaaId) + kitongojis.filter { it.mtaaId == mtaaId }).sortedWith(compareBy({ it.name.lowercase() }, { it.id }))

    override suspend fun byPostcode(postcode: String): AddressPath? =
        if (postcode in wardsByPostcode) paths[Level.WARD to postcode] else base.byPostcode(postcode)

    override suspend fun byPrefix(prefix: String): List<AddressPath> {
        val fromBase = base.byPrefix(prefix)
        if (prefix.length !in PREFIX_LENGTHS || !prefix.all { it in '0'..'9' }) return fromBase
        val extra = wards.filter { it.postcode.startsWith(prefix) }.mapNotNull { paths[Level.WARD to it.postcode] }
        return (fromBase + extra).sortedBy { it.ward!!.postcode }
    }

    override suspend fun path(level: Level, id: String): AddressPath? = when (level) {
        Level.REGION, Level.DISTRICT -> base.path(level, id)
        Level.WARD -> if (id in wardsByPostcode) paths[level to id] else base.path(level, id)
        Level.MTAA -> if (id in mtaasById) paths[level to id] else base.path(level, id)
        Level.KITONGOJI -> if (id in kitongojisById) paths[level to id] else base.path(level, id)
    }

    override fun isValidPostcode(postcode: String): Boolean = postcode in wardsByPostcode || base.isValidPostcode(postcode)

    override suspend fun search(query: String, limit: Int, levels: Set<Level>): List<AddressMatch> {
        val queryTokens = AddressText.tokens(query)
        if (queryTokens.isEmpty() || levels.isEmpty()) return emptyList()
        val max = SearchQuery.clampLimit(limit)
        val found = LinkedHashMap<Pair<Level, String>, AddressMatch>()
        base.search(query, max, levels).forEach { found[it.level to idOf(it.level, it.path)] = it }
        extraNodes().filter { it.first in levels }.forEach { (level, id) ->
            val path = paths[level to id] ?: return@forEach
            val haystack = listOfNotNull(path.region.name, path.district?.name, path.ward?.name, path.mtaa?.name, path.kitongoji?.name)
                .flatMap { AddressText.tokens(it) }
            if (queryTokens.all { t -> haystack.any { it.startsWith(t) } }) {
                val label = PathQueries.label(level, path)
                found[level to id] = AddressMatch(path, level, label, path.ward?.postcode, SearchRanking.score(query, label))
            }
        }
        return found.values
            .map { SearchRanking.Ranked(it.score, it.level, AddressText.normalize(it.label), idOf(it.level, it.path)) to it }
            .sortedWith { a, b -> SearchRanking.order.compare(a.first, b.first) }
            .take(max)
            .map { it.second }
    }

    override fun close() = base.close()

    /** Resolves every extra place's path, wards first, so an mtaa under an extra ward (and a kitongoji under an extra mtaa) finds its parent. */
    private suspend fun resolvePaths() {
        for (ward in wards) base.path(Level.DISTRICT, ward.districtCode)?.copy(ward = ward)?.let { paths[Level.WARD to ward.postcode] = it }
        for (mtaa in mtaas) {
            val parent = paths[Level.WARD to mtaa.wardPostcode] ?: base.path(Level.WARD, mtaa.wardPostcode)
            parent?.copy(mtaa = mtaa)?.let { paths[Level.MTAA to mtaa.id] = it }
        }
        for (kitongoji in kitongojis) {
            val parent = paths[Level.MTAA to kitongoji.mtaaId] ?: base.path(Level.MTAA, kitongoji.mtaaId)
            parent?.copy(kitongoji = kitongoji)?.let { paths[Level.KITONGOJI to kitongoji.id] = it }
        }
    }

    private fun extraNodes(): List<Pair<Level, String>> =
        wards.map { Level.WARD to it.postcode } + mtaas.map { Level.MTAA to it.id } + kitongojis.map { Level.KITONGOJI to it.id }

    private fun idOf(level: Level, path: AddressPath): String = when (level) {
        Level.REGION -> path.region.code
        Level.DISTRICT -> path.district!!.code
        Level.WARD -> path.ward!!.postcode
        Level.MTAA -> path.mtaa!!.id
        Level.KITONGOJI -> path.kitongoji!!.id
    }

    companion object {
        private val PREFIX_LENGTHS = setOf(2, 3, 5)

        /** Validates [extras] against [base] and layers them on it; [base] is closed when that fails. */
        suspend fun create(base: AddressStore, extras: List<ExtraPlace>): LayeredAddressStore = try {
            LayeredAddressStore(base, ExtraPlaceValidator.resolve(base, extras)).also { it.resolvePaths() }
        } catch (e: Throwable) {
            base.close()
            throw e
        }
    }
}
