package com.omarshehe.tzaddress.importer

/** Cells keyed by column role; words that fall left of every column are [unassigned]. */
data class Row(val cells: Map<Role, String>, val unassigned: List<Word>)
