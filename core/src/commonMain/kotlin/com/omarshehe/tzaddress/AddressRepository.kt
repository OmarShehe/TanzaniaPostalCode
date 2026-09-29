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
}
