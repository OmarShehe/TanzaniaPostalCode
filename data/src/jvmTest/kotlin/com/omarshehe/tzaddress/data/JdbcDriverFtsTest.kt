package com.omarshehe.tzaddress.data

import androidx.sqlite.execSQL
import kotlin.test.Test
import kotlin.test.assertEquals

/** The FTS5 prefix query the search depends on, run on the driver production uses (it must work on every host, Intel macOS included). */
class JdbcDriverFtsTest {
    @Test
    fun fts5PrefixQueryWorksWithTheProductionDriver() {
        val connection = JdbcSQLiteDriver().open(":memory:")
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
