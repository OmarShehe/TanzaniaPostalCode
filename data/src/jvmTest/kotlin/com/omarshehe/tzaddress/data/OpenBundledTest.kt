package com.omarshehe.tzaddress.data

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class OpenBundledTest {
    @Test fun installsFromClasspathResourcesAndOpens() = runBlocking {
        val dir = Files.createTempDirectory("bundled").toFile()
        val repository = openBundled(dir.path, JdbcSQLiteDriver(), ::readBundledResource)
        assertEquals(30, repository.regions().size)
        assertEquals("Kivukoni", repository.byPostcode("11101")?.ward?.name)
    }
}
