package com.omarshehe.tzaddress.data

/** Thrown when creating a store with extra places that cannot be applied; [problems] lists every one and nothing was applied. */
public class ExtraPlacesInvalidException(public val problems: List<String>) :
    IllegalArgumentException("Invalid extra places (${problems.size}):\n" + problems.joinToString("\n") { "- $it" })
