package com.omarshehe.tzaddress

/** Levels of the address hierarchy, from the top down. */
public enum class Level {
    REGION, DISTRICT, WARD, MTAA, KITONGOJI;

    public companion object {
        public val all: Set<Level> = entries.toSet()
    }
}
