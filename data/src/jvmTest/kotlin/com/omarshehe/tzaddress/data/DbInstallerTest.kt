package com.omarshehe.tzaddress.data

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class DbInstallerTest {
    private val dir = Files.createTempDirectory("installer").toFile()
    private val db = File(TestDb.path)
    private val realStamp = File(TestDb.path + ".version").readText()
    private var bytesReads = 0

    private fun installer(stamp: String = realStamp, directory: File = File(dir, "nested/tz")) = DbInstaller(
        directory = directory.path,
        driver = JdbcSQLiteDriver(),
        bundledStamp = { stamp },
        bundledBytes = { bytesReads++; db.readBytes() },
    )

    @Test fun firstRunCopiesTheDatabase() = runBlocking {
        val path = installer().install()
        assertEquals(1, bytesReads)
        assertTrue(File(path).length() == db.length())
        assertTrue(File(path).parentFile.listFiles()!!.none { it.name.endsWith(".tmp") }, "no temp file left")
        SqliteAddressRepository.open(JdbcSQLiteDriver(), path).also { assertEquals(31, it.regions().size); it.close() }
        Unit
    }

    @Test fun secondRunDoesNotCopyAgain() = runBlocking {
        installer().install()
        installer().install()
        assertEquals(1, bytesReads)
    }

    @Test fun bundledVersionChangeReplacesTheFile() = runBlocking {
        val path = installer().install()
        File(path).writeBytes(File(path).readBytes()) // same content, still the old stamp
        installer(stamp = "2|other|2030-01-01T00:00:00Z").install()
        assertEquals(2, bytesReads)
    }

    @Test fun databaseFromTheFirstDatasetVersionIsReplaced() = runBlocking {
        val directory = File(dir, "v1").apply { mkdirs() }
        val installed = File(directory, "tz-address.db").also { db.copyTo(it) }
        java.sql.DriverManager.getConnection("jdbc:sqlite:${installed.absolutePath}").use { c ->
            c.createStatement().use { it.executeUpdate("UPDATE dataset_info SET version = '1'") }
        }
        val path = installer(directory = directory).install()
        assertEquals(1, bytesReads, "the older database is replaced by the bundled one")
        assertEquals("3", SqliteAddressRepository.open(JdbcSQLiteDriver(), path).let { it.info().version.also { _ -> it.close() } })
    }

    @Test fun corruptInstalledFileIsReplaced() = runBlocking {
        val directory = File(dir, "corrupt").apply { mkdirs() }
        File(directory, "tz-address.db").writeText("not a database")
        val path = installer(directory = directory).install()
        assertEquals(1, bytesReads)
        assertEquals(db.length(), File(path).length())
    }

    @Test fun installsRunningAtTheSameTimeLeaveOneValidDatabase() = runBlocking(Dispatchers.Default) {
        val directory = File(dir, "concurrent")
        val paths = (1..8).map { async { installer(directory = directory).install() } }.awaitAll()
        assertEquals(1, paths.toSet().size)
        assertEquals(db.length(), File(paths.first()).length())
        assertTrue(directory.listFiles()!!.none { it.name.endsWith(".tmp") }, "no temp file left")
        SqliteAddressRepository.open(JdbcSQLiteDriver(), paths.first()).also { assertEquals(31, it.regions().size); it.close() }
        Unit
    }

    @Test fun sidecarStampMatchesTheGeneratedDatabase() = runBlocking {
        assertEquals(realStamp, DbInstaller.readInstalledStamp(JdbcSQLiteDriver(), TestDb.path))
        assertEquals(null, DbInstaller.readInstalledStamp(JdbcSQLiteDriver(), File(dir, "missing.db").path))
    }
}
