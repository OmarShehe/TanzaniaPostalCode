package com.omarshehe.tzaddress.importer

/** How a ward of ours was (or was not) paired with a boundary ward. */
enum class WardMatchKind(val matched: Boolean) {
    /** Exactly one boundary ward has this name inside the ward's district. */
    MATCHED_DISTRICT(true),

    /** The district could not be used, but the name is unique among boundary wards and among ours. */
    MATCHED_UNIQUE_NAME(true),

    /** More than one boundary ward could be meant, or two of our wards want the same one. */
    AMBIGUOUS(false),

    /** No boundary ward has this name. */
    NO_MATCH(false),
}
