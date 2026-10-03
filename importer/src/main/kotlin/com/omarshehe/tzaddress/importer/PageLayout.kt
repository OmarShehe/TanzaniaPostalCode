package com.omarshehe.tzaddress.importer

/** Column boundaries come from this page's own header row; they differ from page to page. */
data class PageLayout(val kind: LayoutKind, val columns: List<Column>) {

    /** The column a cell belongs to: the last header whose x is at most [x] plus a small tolerance. */
    fun roleAt(x: Double): Role? = columns.lastOrNull { it.x <= x + CELL_TOLERANCE }?.role

    /** The label stacked under the header can start left of the "OLD" word; the cells start where the label does. */
    fun withOldColumnNoFurtherRightThan(x: Double): PageLayout = copy(
        columns = columns.map { if (it.role == Role.OLD_WARD_CODE && x < it.x && it.x - x < OLD_LABEL_SHIFT) it.copy(x = x) else it },
    )

    private companion object {
        const val OLD_LABEL_SHIFT = 60.0
        const val CELL_TOLERANCE = 12.0
    }
}
