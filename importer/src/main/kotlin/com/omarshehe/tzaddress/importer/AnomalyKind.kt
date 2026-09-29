package com.omarshehe.tzaddress.importer

/** [informational] kinds record a known quirk of the source that is handled by a rule; they don't count against the failure ratio. */
enum class AnomalyKind(val informational: Boolean = false) {
    KITONGOJI_WITHOUT_MTAA(informational = true),
    BAD_WARD_POSTCODE,
    DISTRICT_WITHOUT_CODE,
    ORPHAN_ROW,
    MISSING_HEADER,
    UNASSIGNED_TEXT,
    UNEXPECTED_CELL,
}
