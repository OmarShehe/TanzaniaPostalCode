package com.omarshehe.tzaddress.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals

/** FTS spike on the JVM with the driver production uses. Skipped (visibly) on hosts the bundled driver has no binary for, e.g. Intel macOS. */
class BundledDriverFtsSpikeTest {
    @Test
    fun fts5PrefixQueryWorksWithBundledDriver() {
        val connection: SQLiteConnection = try {
            BundledSQLiteDriver().open(":memory:")
        } catch (e: Throwable) {
            val unsupported = generateSequence(e) { it.cause }.any { "Cannot find a suitable SQLite binary" in (it.message ?: "") }
            assumeTrue("SKIPPED: bundled SQLite has no binary for this host (${System.getProperty("os.name")} ${System.getProperty("os.arch")})", !unsupported)
            throw e
        }
        try {
            connection.execSQL("CREATE VIRTUAL TABLE t USING fts5(name, prefix='2 3')")
            connection.execSQL("INSERT INTO t(name) VALUES ('kivukoni'), ('kariakoo'), ('kivule')")
            val hits = connection.query("SELECT name FROM t WHERE t MATCH ? ORDER BY name", "\"kiv\"*") { it.getText(0) }
            assertEquals(listOf("kivukoni", "kivule"), hits)
        } finally {
            connection.close()
        }
    }
}
