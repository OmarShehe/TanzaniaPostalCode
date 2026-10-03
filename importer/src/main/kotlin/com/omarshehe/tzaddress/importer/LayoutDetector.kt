package com.omarshehe.tzaddress.importer

object LayoutDetector {

    private val BANNER = Regex("^(.+?)\\s+REGION\\s*[-\\u2010-\\u2015\\u2212]\\s*(\\d{5})$")
    private val HEADER_TOKENS = setOf("REGION", "POSTCODE", "DISTRICT", "WARD", "MTAA/VILLAGE", "KITONGOJI", "SHEHIA")

    private val ROLES = mapOf(
        LayoutKind.A to listOf(Role.DISTRICT_NAME, Role.DISTRICT_CODE, Role.WARD_NAME, Role.WARD_CODE, Role.MTAA, Role.KITONGOJI),
        LayoutKind.B to listOf(
            Role.REGION_NAME, Role.REGION_CODE, Role.DISTRICT_NAME, Role.DISTRICT_CODE,
            Role.WARD_NAME, Role.WARD_CODE, Role.MTAA, Role.KITONGOJI,
        ),
        LayoutKind.Z to listOf(
            Role.REGION_NAME, Role.REGION_CODE, Role.DISTRICT_NAME, Role.DISTRICT_CODE,
            Role.WARD_NAME, Role.WARD_CODE, Role.MTAA,
        ),
    )

    /** Splits a page at every banner or repeated header, so a region that starts mid-page gets its own segment. */
    fun detect(lines: List<Line>): PageStructure {
        if (lines.isEmpty()) return PageStructure(PageKind.BLANK, null, null, emptyList())
        val finished = ArrayList<PageSegment>()
        var current = SegmentBuilder()
        for (line in lines) {
            val banner = BANNER.matchEntire(line.text.trim())?.let { Banner(it.groupValues[1].trim(), it.groupValues[2]) }
            when {
                banner != null -> {
                    if (!current.isFresh()) {
                        finished += current.build()
                        current = SegmentBuilder()
                    }
                    current.banner = banner
                }
                isHeader(line) -> {
                    if (current.hasHeader) {
                        finished += current.build()
                        current = SegmentBuilder()
                    }
                    current.hasHeader = true
                    current.layout = layoutOf(line)
                }
                else -> current.body += line
            }
        }
        finished += current.build()
        val first = finished.first()
        return PageStructure(PageKind.CONTENT, first.banner, first.layout, first.body, finished.drop(1))
    }

    private class SegmentBuilder {
        var banner: Banner? = null
        var layout: PageLayout? = null
        var hasHeader = false
        val body = ArrayList<Line>()

        fun isFresh() = banner == null && !hasHeader && body.isEmpty()
        fun build() = PageSegment(banner, layout, body.toList())
    }

    private fun isHeader(line: Line): Boolean {
        val texts = line.words.map { it.text }
        return "POSTCODE" in texts && ("MTAA/VILLAGE" in texts || "SHEHIA" in texts)
    }

    private fun layoutOf(header: Line): PageLayout? {
        val tokens = header.words.filter { it.text in HEADER_TOKENS }
        val texts = tokens.map { it.text }
        val kind = when {
            "SHEHIA" in texts -> LayoutKind.Z
            "DISTRICT" in texts -> LayoutKind.B
            else -> LayoutKind.A
        }
        val roles = ROLES.getValue(kind)
        if (tokens.size != roles.size) return null
        return PageLayout(kind, tokens.mapIndexed { i, word -> Column(roles[i], word.x0) })
    }
}
