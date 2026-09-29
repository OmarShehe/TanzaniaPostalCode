package com.omarshehe.tzaddress.importer

data class PageStructure(
    val kind: PageKind,
    val banner: Banner?,
    val layout: PageLayout?,
    val body: List<Line>,
)
