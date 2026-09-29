package com.omarshehe.tzaddress.importer

/** Column boundaries come from this page's own header row; they differ from page to page. */
data class PageLayout(val kind: LayoutKind, val columns: List<Column>) {

    /** The column a cell belongs to: the last header whose x is at most [x] plus a small tolerance. */
    fun roleAt(x: Double): Role? = columns.lastOrNull { it.x <= x + CELL_TOLERANCE }?.role

    private companion object {
        const val CELL_TOLERANCE = 12.0
    }
}
