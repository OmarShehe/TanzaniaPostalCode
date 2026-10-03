package com.omarshehe.tzaddress.importer

/** Known breaks in TCRA's files that the parser cannot tell from a real name; each applied correction is reported. */
object SourceCorrections {
    /** District code to the name as the file prints it and the name it stands for (broken in the middle of a word, or stray punctuation). */
    private val brokenDistrictNames = mapOf(
        "593" to ("WANGING'O MBE" to "Wanging'ombe"),
        "501" to ("MPANDA -CBD" to "Mpanda CBD"),
    )

    fun apply(result: BuildResult): List<String> {
        val notes = ArrayList<String>()
        for (district in result.regions.flatMap { it.districts }) {
            val (printed, fixed) = brokenDistrictNames[district.code] ?: continue
            if (district.rawName.equals(printed, ignoreCase = true)) {
                district.rawName = fixed
                notes += "District ${district.code} printed as '$printed', read as '$fixed'"
            }
        }
        return notes
    }
}
