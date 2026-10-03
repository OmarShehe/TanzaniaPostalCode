package com.omarshehe.tzaddress.importer

/** `dataset/edition-changes.md`: sorted throughout, so a second run gives the same file. */
object EditionChangesReport {

    fun markdown(changes: EditionChanges, previousEdition: String, currentEdition: String): String = buildString {
        appendLine("# Edition changes")
        appendLine()
        appendLine("- Previous edition: $previousEdition")
        appendLine("- This edition: $currentEdition")
        appendLine()
        appendLine("Wards are matched by postcode; a ward whose new list gives an old postcode is re-coded. Ward ids are postcodes, so a re-coded ward has a new id.")
        appendLine()
        appendLine("## Totals")
        appendLine()
        appendLine("- Wards added: ${changes.added.size}")
        appendLine("- Wards removed: ${changes.removed.size}")
        appendLine("- Wards re-coded: ${changes.recoded.size}")
        appendLine("- Wards renamed: ${changes.renamed.size}")
        appendLine("- Regions added: ${changes.addedRegions.size}, removed: ${changes.removedRegions.size}")
        appendLine("- Districts added: ${changes.addedDistricts.size}, removed: ${changes.removedDistricts.size}")
        appendLine()
        appendLine("## Per region")
        appendLine()
        appendLine("| Region | Code | Added | Removed | Re-coded | Renamed |")
        appendLine("|---|---|---|---|---|---|")
        val codes = (changes.added.map { it.regionCode } + changes.removed.map { it.regionCode } + changes.recoded.map { it.regionCode } +
            changes.renamed.map { it.regionCode } + changes.addedRegions.map { it.code } + changes.removedRegions.map { it.code }).toSortedSet()
        for (code in codes) {
            val name = changes.regionNames[code] ?: code
            appendLine("| $name | $code | ${changes.added.count { it.regionCode == code }} | ${changes.removed.count { it.regionCode == code }} | ${changes.recoded.count { it.regionCode == code }} | ${changes.renamed.count { it.regionCode == code }} |")
        }
        section("Regions added", changes.addedRegions.sortedBy { it.code }.map { "${it.code} ${it.name}" })
        section("Regions removed", changes.removedRegions.sortedBy { it.code }.map { "${it.code} ${it.name}" })
        section("Districts added", changes.addedDistricts.sortedBy { it.code }.map { "${it.code} ${it.name} (region ${it.regionCode})" })
        section("Districts removed", changes.removedDistricts.sortedBy { it.code }.map { "${it.code} ${it.name} (region ${it.regionCode})" })
        section("Wards re-coded", changes.recoded.sortedBy { it.newPostcode }.map { "${it.oldPostcode} to ${it.newPostcode} ${it.newName}" })
        section("Wards renamed", changes.renamed.sortedBy { it.postcode }.map { "${it.postcode} ${it.oldName} to ${it.newName}" })
        section("Wards added", changes.added.sortedBy { it.postcode }.map { "${it.postcode} ${it.name}" })
        section("Wards removed", changes.removed.sortedBy { it.postcode }.map { "${it.postcode} ${it.name}" })
    }

    private fun StringBuilder.section(title: String, lines: List<String>) {
        if (lines.isEmpty()) return
        appendLine()
        appendLine("## $title (${lines.size})")
        appendLine()
        lines.forEach { appendLine("- $it") }
    }
}
