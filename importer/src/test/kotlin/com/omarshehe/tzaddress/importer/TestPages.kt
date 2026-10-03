package com.omarshehe.tzaddress.importer

/** Hand-built page content for parser tests. y grows downward, like PDFBox. */
object TestPages {

    /** One header geometry: x of each header cell in reading order. */
    val geometryOne = listOf(77.0, 135.0, 195.0, 272.0, 332.0, 416.0, 478.0, 585.0)
    val geometryTwo = listOf(50.0, 121.0, 185.0, 262.0, 334.0, 432.0, 494.0, 627.0)

    fun word(text: String, x: Double, y: Double) = Word(text, x, x + text.length * 7.0, y)

    /** A line made of cells; each cell is (x, text) and multi-word text is split into words. */
    fun line(y: Double, vararg cells: Pair<Double, String>): Line {
        val words = cells.flatMap { (x, text) ->
            var cursor = x
            text.split(" ").map { part ->
                word(part, cursor, y).also { cursor = it.x1 + 4.0 }
            }
        }
        return Line(y, words.sortedBy { it.x0 })
    }

    fun headerB(y: Double, xs: List<Double> = geometryOne) = line(
        y,
        xs[0] to "REGION", xs[1] to "POSTCODE", xs[2] to "DISTRICT", xs[3] to "POSTCODE",
        xs[4] to "WARD", xs[5] to "POSTCODE", xs[6] to "MTAA/VILLAGE", xs[7] to "KITONGOJI",
    )

    fun headerA(y: Double, xs: List<Double> = geometryOne) = line(
        y,
        xs[0] to "REGION", xs[1] to "POSTCODE", xs[4] to "WARD", xs[5] to "POSTCODE",
        xs[6] to "MTAA/VILLAGE", xs[7] to "KITONGOJI",
    )

    fun headerZ(y: Double, xs: List<Double> = geometryOne) = line(
        y,
        xs[0] to "REGION", xs[1] to "POSTCODE", xs[2] to "DISTRICT", xs[3] to "POSTCODE",
        xs[4] to "WARD (DELIVERY AREAS)", xs[5] to "POSTCODE", xs[6] to "SHEHIA",
    )

    fun banner(y: Double, text: String) = line(y, 72.0 to text)

    fun structure(vararg lines: Line): PageStructure = LayoutDetector.detect(lines.toList())
}
