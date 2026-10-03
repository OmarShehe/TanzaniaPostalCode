package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.AddressIds
import com.omarshehe.tzaddress.AddressText
import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.model.ExtraPlace
import com.omarshehe.tzaddress.model.ExtraPlaceEntry
import com.omarshehe.tzaddress.model.ExtraPlaceStatus

/** Checks an app's extra places against the bundled store and gives each its id and status; throws [ExtraPlacesInvalidException] listing every problem. */
internal object ExtraPlaceValidator {
    private const val MIN_LAT = -12.0
    private const val MAX_LAT = -1.0
    private const val MIN_LON = 29.0
    private const val MAX_LON = 41.5
    private val postcodeShape = Regex("^\\d{5}$")

    suspend fun resolve(base: AddressStore, extras: List<ExtraPlace>): List<ExtraPlaceEntry> {
        val problems = ArrayList<String>()
        val usable = BooleanArray(extras.size)
        extras.forEachIndexed { i, place -> usable[i] = checkCommon(i, place, problems) }
        val results = arrayOfNulls<ExtraPlaceEntry>(extras.size)

        // Wards first, then mtaa, then kitongoji, so a child can sit under an extra parent whatever the list order.
        val wardsByPostcode = HashMap<String, ExtraPlaceEntry>()
        val shadowedWards = HashMap<String, String>()
        val seenWardNames = HashSet<Pair<String, String>>()
        val seenPostcodes = HashSet<String>()
        for (i in extras.indices.filter { usable[it] && extras[it].level == Level.WARD }) {
            val place = extras[i]
            val postcode = place.postcode ?: continue
            val district = base.path(Level.DISTRICT, place.parentId)
            when {
                !postcodeShape.matches(postcode) -> problems += "${describe(i, place)}: postcode '$postcode' is not five digits"
                district == null -> problems += "${describe(i, place)}: no district with code '${place.parentId}'"
                !postcode.startsWith(place.parentId) -> problems += "${describe(i, place)}: postcode '$postcode' must start with its district code '${place.parentId}'"
                !seenPostcodes.add(postcode) -> problems += "${describe(i, place)}: postcode '$postcode' is used twice in the list"
                !seenWardNames.add(place.parentId to AddressText.normalize(place.name)) -> problems += "${describe(i, place)}: another extra ward of district '${place.parentId}' has this name"
                else -> {
                    val builtIn = if (base.isValidPostcode(postcode)) postcode
                    else base.wards(place.parentId).firstOrNull { AddressText.normalize(it.name) == AddressText.normalize(place.name) }?.postcode
                    if (builtIn != null) {
                        if (builtIn != postcode) shadowedWards[postcode] = builtIn
                        results[i] = ExtraPlaceEntry(place, postcode, ExtraPlaceStatus.SHADOWED)
                    } else {
                        ExtraPlaceEntry(place, postcode, ExtraPlaceStatus.ACTIVE).also { wardsByPostcode[postcode] = it; results[i] = it }
                    }
                }
            }
        }

        val mtaasById = HashMap<String, ExtraPlaceEntry>()
        val shadowedMtaas = HashMap<String, String>()
        resolveChildren(base, extras, usable, results, problems, Level.MTAA, mtaasById, shadowedMtaas,
            parentExists = { parent -> base.isValidPostcode(parent) || wardsByPostcode.containsKey(parent) },
            shadowedParent = { parent -> shadowedWards[parent] },
            builtInChildren = { parent -> base.mtaas(parent).map { it.id to it.name } },
            parentKind = "ward",
        )
        resolveChildren(base, extras, usable, results, problems, Level.KITONGOJI, HashMap(), HashMap(),
            parentExists = { parent -> base.path(Level.MTAA, parent) != null || mtaasById.containsKey(parent) },
            shadowedParent = { parent -> shadowedMtaas[parent] },
            builtInChildren = { parent -> base.kitongojis(parent).map { it.id to it.name } },
            parentKind = "mtaa",
        )

        if (problems.isNotEmpty()) throw ExtraPlacesInvalidException(problems)
        return results.map { it!! }
    }

    private suspend fun resolveChildren(
        base: AddressStore,
        extras: List<ExtraPlace>,
        usable: BooleanArray,
        results: Array<ExtraPlaceEntry?>,
        problems: MutableList<String>,
        level: Level,
        activeById: MutableMap<String, ExtraPlaceEntry>,
        shadowedById: MutableMap<String, String>,
        parentExists: suspend (String) -> Boolean,
        shadowedParent: (String) -> String?,
        builtInChildren: suspend (String) -> List<Pair<String, String>>,
        parentKind: String,
    ) {
        val seenNames = HashSet<Pair<String, String>>()
        val usedIds = HashMap<String, MutableSet<String>>()
        for (i in extras.indices.filter { usable[it] && extras[it].level == level }) {
            val place = extras[i]
            val parent = place.parentId
            val normalized = AddressText.normalize(place.name)
            val shadow = shadowedParent(parent)
            when {
                shadow != null -> problems += "${describe(i, place)}: parent $parentKind '$parent' is shadowed by a built-in place; use '$shadow' instead"
                !parentExists(parent) -> problems += "${describe(i, place)}: no $parentKind with ${if (parentKind == "ward") "postcode" else "id"} '$parent'"
                !seenNames.add(parent to normalized) -> problems += "${describe(i, place)}: another extra ${level.name.lowercase()} of '$parent' has this name"
                else -> {
                    val builtIn = builtInChildren(parent)
                    val same = builtIn.firstOrNull { AddressText.normalize(it.second) == normalized }
                    if (same != null) {
                        if (deriveBase(parent, place.name) != same.first) shadowedById[deriveBase(parent, place.name)] = same.first
                        results[i] = ExtraPlaceEntry(place, same.first, ExtraPlaceStatus.SHADOWED)
                    } else {
                        val used = usedIds.getOrPut(parent) { builtIn.mapTo(HashSet()) { it.first } }
                        val id = uniqueId(parent, place.name, used)
                        ExtraPlaceEntry(place, id, ExtraPlaceStatus.ACTIVE).also { activeById[id] = it; results[i] = it }
                    }
                }
            }
        }
    }

    private fun deriveBase(parent: String, name: String) = "$parent/${AddressIds.slug(name)}"

    private fun uniqueId(parent: String, name: String, used: MutableSet<String>): String {
        val base = deriveBase(parent, name)
        var candidate = base
        var n = 2
        while (!used.add(candidate)) candidate = "$base-${n++}"
        return candidate
    }

    /** Rules that need nothing from the store; returns false only when the level is unsupported, so every other problem of an entry is still listed. */
    private fun checkCommon(index: Int, place: ExtraPlace, problems: MutableList<String>): Boolean {
        val label = describe(index, place)
        if (place.name.isBlank()) problems += "$label: the name is blank"
        if (place.level != Level.WARD && place.level != Level.MTAA && place.level != Level.KITONGOJI) {
            problems += "$label: level ${place.level} is not supported (only WARD, MTAA and KITONGOJI)"
            return false
        }
        if (place.level != Level.WARD) {
            if (place.postcode != null) problems += "$label: only a ward has a postcode"
            if (place.latitude != null || place.longitude != null) problems += "$label: only a ward has a position"
        } else {
            if (place.postcode == null) problems += "$label: a ward needs a postcode"
            val lat = place.latitude
            val lon = place.longitude
            when {
                (lat == null) != (lon == null) -> problems += "$label: give both latitude and longitude, or neither"
                lat != null && lon != null && (lat !in MIN_LAT..MAX_LAT || lon !in MIN_LON..MAX_LON) ->
                    problems += "$label: position ($lat, $lon) is outside Tanzania"
            }
        }
        return true
    }

    private fun describe(index: Int, place: ExtraPlace) = "Extra place #${index + 1} '${place.name.trim()}' (${place.level})"
}
