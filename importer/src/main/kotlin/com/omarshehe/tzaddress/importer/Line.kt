package com.omarshehe.tzaddress.importer

data class Line(val y: Double, val words: List<Word>) {
    val text: String get() = words.joinToString(" ") { it.text }
}
