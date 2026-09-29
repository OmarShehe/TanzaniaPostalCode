package com.omarshehe.tzaddress.importer

import java.io.File
import java.util.Calendar
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts

/** Builds tiny PDFs with text at exact positions, so no binary fixtures are committed. */
object PdfFixtureBuilder {

    /** [yTop] is measured from the top of the page, like the extractor reports it. */
    data class Placed(val x: Double, val yTop: Double, val text: String)

    fun build(file: File, pages: List<List<Placed>>, created: Calendar? = null) {
        PDDocument().use { doc ->
            val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)
            pages.forEach { placed ->
                val page = PDPage(PDRectangle.LETTER)
                doc.addPage(page)
                if (placed.isNotEmpty()) {
                    PDPageContentStream(doc, page).use { cs ->
                        placed.forEach { p ->
                            cs.beginText()
                            cs.setFont(font, 10f)
                            cs.newLineAtOffset(p.x.toFloat(), (PDRectangle.LETTER.height - p.yTop).toFloat())
                            cs.showText(p.text)
                            cs.endText()
                        }
                    }
                }
            }
            created?.let { doc.documentInformation.creationDate = it }
            doc.save(file)
        }
    }

    /** Places every word of a [Line] at its own x, so tests can reuse [TestPages]. */
    fun placed(line: Line): List<Placed> = line.words.map { Placed(it.x0, line.y, it.text) }
}
