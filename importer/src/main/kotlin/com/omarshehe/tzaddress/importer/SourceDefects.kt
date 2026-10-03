package com.omarshehe.tzaddress.importer

/** Wards whose postcode in the source is malformed or does not start with the district code; they are left out and reported. */
object SourceDefects {

    fun remove(result: BuildResult): List<String> {
        val defects = ArrayList<String>()
        for (region in result.regions) {
            for (district in region.districts) {
                val iterator = district.wards.iterator()
                while (iterator.hasNext()) {
                    val ward = iterator.next()
                    val reason = when {
                        !PostcodeRules.ward.matches(ward.postcode) -> "postcode is not 5 digits"
                        PostcodeRules.district.matches(district.code) && !ward.postcode.startsWith(district.code) ->
                            "does not start with its district code ${district.code}"
                        else -> null
                    }
                    if (reason != null) {
                        defects += "Ward '${ward.name}' (postcode '${ward.postcode}', ${district.name}, ${region.name}) left out: $reason"
                        iterator.remove()
                    }
                }
            }
        }
        return defects
    }
}
