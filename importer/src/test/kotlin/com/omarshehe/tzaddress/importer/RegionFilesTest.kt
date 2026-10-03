package com.omarshehe.tzaddress.importer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegionFilesTest {

    @Test fun nameAndCodeComeFromTheFileName() {
        assertEquals(Banner("Arusha", "23000"), RegionFiles.banner(File("Arusha_23000.pdf")))
    }

    @Test fun underscoresInTheNameBecomeSpaces() {
        assertEquals(Banner("Dar Es Salaam", "11000"), RegionFiles.banner(File("/x/Dar_Es_Salaam_11000.pdf")))
    }

    @Test fun aNameWithoutAFiveDigitCode_isRejected() {
        assertFailsWith<IllegalArgumentException> { RegionFiles.banner(File("Arusha.pdf")) }
        assertFailsWith<IllegalArgumentException> { RegionFiles.banner(File("Arusha_230.pdf")) }
    }

    @Test fun listIsSortedByCodeAndIgnoresOtherFiles() {
        val dir = kotlin.io.path.createTempDirectory("regions").toFile()
        listOf("Tanga_21000.pdf", "Arusha_23000.pdf", "notes.txt", "Dar_Es_Salaam_11000.pdf").forEach { File(dir, it).writeText("x") }
        assertEquals(listOf("Dar_Es_Salaam_11000.pdf", "Tanga_21000.pdf", "Arusha_23000.pdf"), RegionFiles.list(dir).map { it.name })
        dir.deleteRecursively()
    }
}
