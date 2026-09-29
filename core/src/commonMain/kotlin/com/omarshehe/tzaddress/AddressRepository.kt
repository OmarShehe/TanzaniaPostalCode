package com.omarshehe.tzaddress

import com.omarshehe.tzaddress.model.DatasetInfo
import com.omarshehe.tzaddress.model.District
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import com.omarshehe.tzaddress.model.Region
import com.omarshehe.tzaddress.model.Ward

/** Browse access to the Tanzanian address hierarchy. Unknown parents yield empty lists. */
public interface AddressRepository {
    public suspend fun info(): DatasetInfo
    public suspend fun regions(): List<Region>
    public suspend fun districts(regionCode: String): List<District>
    public suspend fun wards(districtCode: String): List<Ward>
    public suspend fun mtaas(wardPostcode: String): List<Mtaa>
    public suspend fun kitongojis(mtaaId: String): List<Kitongoji>

    /**
     * Ranked type-ahead search over names and ancestor names. A blank query returns an empty list;
     * [limit] is clamped to 1..100.
     */
    public suspend fun search(query: String, limit: Int = 20, levels: Set<Level> = Level.all): List<AddressMatch>

    /** Full chain for a 5-digit ward postcode; null when unknown or malformed. */
    public suspend fun byPostcode(postcode: String): AddressPath?

    /** All wards whose postcode starts with a 2-, 3- or 5-digit [prefix] (region, district or ward code); empty for bad input. */
    public suspend fun byPrefix(prefix: String): List<AddressPath>

    /** Reverse lookup: the chain of any node, addressed by its code/postcode/id. */
    public suspend fun path(level: Level, id: String): AddressPath?

    /** True when [postcode] is a ward postcode of the dataset. */
    public fun isValidPostcode(postcode: String): Boolean
}
