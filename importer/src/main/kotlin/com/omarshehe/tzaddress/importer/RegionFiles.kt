package com.omarshehe.tzaddress.importer

import java.io.File

/** TCRA's regional files are named `<Region>_<code>.pdf`, for example `Dar_Es_Salaam_11000.pdf`. */
object RegionFiles {
    private val NAME = Regex("^(.+)_(\\d{5})\\.pdf$", RegexOption.IGNORE_CASE)

    fun banner(file: File): Banner {
        val match = NAME.matchEntire(file.name)
            ?: throw IllegalArgumentException("Region file '${file.name}' is not named <Region>_<5-digit code>.pdf")
        return Banner(match.groupValues[1].replace('_', ' '), match.groupValues[2])
    }

    fun list(dir: File): List<File> =
        dir.listFiles { f -> f.isFile && NAME.matches(f.name) }.orEmpty().sortedBy { banner(it).regionCode }
}
