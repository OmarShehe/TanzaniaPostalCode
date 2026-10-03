package com.omarshehe.tzaddress.importer

/** The first segment of the page is flattened into [banner], [layout] and [body]; any further segments are in [later]. */
data class PageStructure(
    val kind: PageKind,
    val banner: Banner?,
    val layout: PageLayout?,
    val body: List<Line>,
    val later: List<PageSegment> = emptyList(),
)
