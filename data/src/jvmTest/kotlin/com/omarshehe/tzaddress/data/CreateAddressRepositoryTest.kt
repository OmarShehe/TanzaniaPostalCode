package com.omarshehe.tzaddress.data

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class CreateAddressRepositoryTest {
    @Test fun publicEntryPointInstallsAndOpensTheBundledDatabase() = runBlocking {
        val dir = Files.createTempDirectory("public-entry").toFile()
        val store = createAddressRepository(dir.path)
        try {
            assertEquals(30, store.regions().size)
            assertEquals("Kivukoni", store.byPostcode("11101")?.ward?.name)
        } finally {
            store.close()
        }
    }

    @Test fun aWorkingFolderWithUrlCharactersStillOpens() = runBlocking {
        val dir = Files.createTempDirectory("odd?name=1&x").toFile()
        val store = createAddressRepository(dir.path)
        try {
            assertEquals(30, store.regions().size)
        } finally {
            store.close()
        }
    }

    @Test fun aNullColumnReadsAsEmptyText() {
        val connection = JdbcSQLiteDriver().open(":memory:")
        try {
            val values = connection.query("SELECT NULL") { it.getText(0) }
            assertEquals(listOf(""), values)
        } finally {
            connection.close()
        }
    }
}
