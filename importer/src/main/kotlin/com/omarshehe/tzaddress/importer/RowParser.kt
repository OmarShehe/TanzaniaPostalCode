package com.omarshehe.tzaddress.importer

object RowParser {
    fun parse(line: Line, layout: PageLayout): Row {
        val byRole = LinkedHashMap<Role, MutableList<Word>>()
        val unassigned = ArrayList<Word>()
        for (word in line.words.sortedBy { it.x0 }) {
            val role = layout.roleAt(word.x0)
            if (role == null) unassigned += word else byRole.getOrPut(role) { ArrayList() } += word
        }
        return Row(byRole.mapValues { (_, words) -> words.joinToString(" ") { it.text } }, unassigned)
    }
}
