package com.omarshehe.tzaddress.importer

/** Part of a page under one banner/header: [banner] is null when the region continues from above. */
data class PageSegment(val banner: Banner?, val layout: PageLayout?, val body: List<Line>)
