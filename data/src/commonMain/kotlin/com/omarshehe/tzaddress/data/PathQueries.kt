package com.omarshehe.tzaddress.data

import androidx.sqlite.SQLiteStatement
import com.omarshehe.tzaddress.AddressPath
import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.model.District
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import com.omarshehe.tzaddress.model.Region
import com.omarshehe.tzaddress.model.Ward

/** One joined SELECT per level that returns the node and all its ancestors. */
internal object PathQueries {

    /** Selects the chain for the node whose key column equals `?`. */
    fun sql(level: Level): String {
        val columns = mutableListOf("r.code", "r.name")
        val joins = StringBuilder("FROM region r")
        if (level >= Level.DISTRICT) {
            columns += listOf("d.code", "d.name", "d.region_code")
            joins.append(" JOIN district d ON d.region_code = r.code")
        }
        if (level >= Level.WARD) {
            columns += listOf("w.postcode", "w.name", "w.district_code", "w.latitude", "w.longitude")
            joins.append(" JOIN ward w ON w.district_code = d.code")
        }
        if (level >= Level.MTAA) {
            columns += listOf("m.id", "m.name", "m.ward_postcode")
            joins.append(" JOIN mtaa m ON m.ward_postcode = w.postcode")
        }
        if (level >= Level.KITONGOJI) {
            columns += listOf("k.id", "k.name", "k.mtaa_id")
            joins.append(" JOIN kitongoji k ON k.mtaa_id = m.id")
        }
        return "SELECT ${columns.joinToString()} $joins WHERE ${KEY.getValue(level)} = ?"
    }

    fun read(level: Level, row: SQLiteStatement): AddressPath = AddressPath(
        region = Region(row.getText(0), row.getText(1)),
        district = if (level >= Level.DISTRICT) District(row.getText(2), row.getText(3), row.getText(4)) else null,
        ward = if (level >= Level.WARD) Ward(row.getText(5), row.getText(6), row.getText(7), row.getNullableDouble(8), row.getNullableDouble(9)) else null,
        mtaa = if (level >= Level.MTAA) Mtaa(row.getText(10), row.getText(11), row.getText(12)) else null,
        kitongoji = if (level >= Level.KITONGOJI) Kitongoji(row.getText(13), row.getText(14), row.getText(15)) else null,
    )

    /** Display name of the node a path was resolved for. */
    fun label(level: Level, path: AddressPath): String = when (level) {
        Level.REGION -> path.region.name
        Level.DISTRICT -> path.district!!.name
        Level.WARD -> path.ward!!.name
        Level.MTAA -> path.mtaa!!.name
        Level.KITONGOJI -> path.kitongoji!!.name
    }

    private val KEY = mapOf(
        Level.REGION to "r.code",
        Level.DISTRICT to "d.code",
        Level.WARD to "w.postcode",
        Level.MTAA to "m.id",
        Level.KITONGOJI to "k.id",
    )
}
