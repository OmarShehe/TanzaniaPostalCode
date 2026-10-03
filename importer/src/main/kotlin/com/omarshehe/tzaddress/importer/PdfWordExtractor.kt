package com.omarshehe.tzaddress.importer

import java.io.File
import java.util.Calendar
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition

/** Reads every word of a PDF with its position; y grows downward. */
object PdfWordExtractor {
    private const val LINE_TOLERANCE = 5.0
    private const val WORD_GAP_FACTOR = 0.2

    fun extract(file: File): ExtractedPdf = Loader.loadPDF(file).use { doc ->
        val collector = WordCollector()
        collector.sortByPosition = true
        collector.getText(doc)
        val byPage = collector.words.groupBy { it.first }
        val pages = (1..doc.numberOfPages).map { page ->
            PageWords(page, groupIntoLines(byPage[page].orEmpty().map { it.second }))
        }
        ExtractedPdf(pages, doc.documentInformation.creationDate?.let(::isoDate))
    }

    fun groupIntoLines(words: List<Word>): List<Line> {
        val lines = ArrayList<MutableList<Word>>()
        var lineY = Double.NaN
        for (word in words.sortedWith(compareBy({ it.y }, { it.x0 }))) {
            if (lines.isEmpty() || word.y - lineY > LINE_TOLERANCE) {
                lines += ArrayList<Word>()
                lineY = word.y
            }
            lines.last() += word
        }
        return lines.map { Line(it.first().y, it.sortedBy { w -> w.x0 }) }
    }

    private fun isoDate(calendar: Calendar) =
        "%04d-%02d-%02d".format(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))

    private class WordCollector : PDFTextStripper() {
        val words = ArrayList<Pair<Int, Word>>()

        override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
            val buffer = StringBuilder()
            var start: TextPosition? = null
            var previous: TextPosition? = null
            fun flush() {
                val first = start ?: return
                val last = previous ?: return
                words += currentPageNo to Word(buffer.toString(), first.xDirAdj.toDouble(), (last.xDirAdj + last.widthDirAdj).toDouble(), first.yDirAdj.toDouble())
                buffer.setLength(0)
                start = null
            }
            for (position in textPositions) {
                val unicode = position.unicode
                if (unicode.isBlank()) {
                    flush()
                    previous = null
                    continue
                }
                val before = previous
                if (before != null && position.xDirAdj - (before.xDirAdj + before.widthDirAdj) > WORD_GAP_FACTOR * position.fontSizeInPt) flush()
                if (start == null) start = position
                buffer.append(unicode)
                previous = position
            }
            flush()
        }
    }
}
