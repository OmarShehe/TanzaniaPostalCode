package com.omarshehe.tzaddress.data

import android.util.Log
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.system.measureNanoTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/** Runs on a real device: FTS with the bundled driver, then the whole bundled-DB path and the search benchmark. */
class BundledDriverDeviceTest {

    @Test
    fun fts5PrefixQueryWorksWithBundledDriver() {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL("CREATE VIRTUAL TABLE t USING fts5(name, prefix='2 3')")
            connection.execSQL("INSERT INTO t(name) VALUES ('kivukoni'), ('kariakoo'), ('kivule')")
            val hits = connection.query("SELECT name FROM t WHERE t MATCH ? ORDER BY name", "\"kiv\"*") { it.getText(0) }
            assertEquals(listOf("kivukoni", "kivule"), hits)
        } finally {
            connection.close()
        }
    }

    @Test
    fun installsBundledDatabaseAndSearches(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = createAddressRepository(context)
        val ward = repository.search("kivu").first { it.label == "Kivukoni" && it.postcode == "11101" }
        assertEquals("Dar es Salaam", ward.path.region.name)
        assertEquals("Ilala CBD", ward.path.district?.name)
        assertEquals(31, repository.regions().size)
        assertTrue(repository.isValidPostcode("11101"))
    }

    @Test
    fun benchmarkThreeLetterPrefixSearch(): Unit = runBlocking {
        val repository = createAddressRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val prefixes = listOf("kiv", "kar", "mji", "mag", "ngo", "uru", "mto", "ban", "sha", "ilu", "mwa", "kib")
        repeat(3) { prefixes.forEach { repository.search(it) } } // warm-up
        val timesMs = buildList {
            repeat(10) { prefixes.forEach { p -> add(measureNanoTime { runBlocking { repository.search(p) } } / 1_000_000.0) } }
        }.sorted()
        val p95 = timesMs[(timesMs.size * 0.95).toInt().coerceAtMost(timesMs.size - 1)]
        Log.i("TzAddressBench", "search 3-letter prefix: n=${timesMs.size} p50=${timesMs[timesMs.size / 2]}ms p95=${p95}ms max=${timesMs.last()}ms")
    }
}
