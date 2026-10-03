package com.omarshehe.tzaddress.importer

/** [creationDate] is the PDF metadata date as `yyyy-MM-dd`, or null when absent. */
data class ExtractedPdf(val pages: List<PageWords>, val creationDate: String?)
