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

    fun detect(lines: List<Line>): PageStructure {
        if (lines.isEmpty()) return PageStructure(PageKind.BLANK, null, null, emptyList())
        var banner: Banner? = null
        var layout: PageLayout? = null
        val body = ArrayList<Line>()
        for (line in lines) {
            val match = BANNER.matchEntire(line.text.trim())
            when {
                match != null && banner == null -> banner = Banner(match.groupValues[1].trim(), match.groupValues[2])
                isHeader(line) -> if (layout == null) layout = layoutOf(line)
                else -> body += line
            }
        }
        return PageStructure(PageKind.CONTENT, banner, layout, body)
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
