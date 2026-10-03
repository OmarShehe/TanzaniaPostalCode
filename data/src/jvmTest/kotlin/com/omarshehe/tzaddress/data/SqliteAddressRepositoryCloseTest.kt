package com.omarshehe.tzaddress.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/** Wraps the test driver so a test can hold one query in flight and count how often the connection is closed. */
private class ProbeDriver : SQLiteDriver {
    override val hasConnectionPool: Boolean = false
    val closeCalls = AtomicInteger()
    val queryEntered = CountDownLatch(1)

    private val latch = CountDownLatch(1)

    @Volatile
    private var armed = false

    /** The next query blocks until [release] is called. Arm it after opening, because opening runs queries too. */
    fun holdNextQuery() { armed = true }
    fun release() { latch.countDown() }

    override fun open(fileName: String): SQLiteConnection {
        val inner = JdbcSQLiteDriver().open(fileName)
        return object : SQLiteConnection by inner {
            override fun prepare(sql: String): SQLiteStatement {
                if (armed) {
                    armed = false
                    queryEntered.countDown()
                    check(latch.await(10, TimeUnit.SECONDS)) { "the test never released the query" }
                }
                return inner.prepare(sql)
            }

            override fun close() {
                closeCalls.incrementAndGet()
                inner.close()
            }
        }
    }
}

class SqliteAddressRepositoryCloseTest {
    private val driver = ProbeDriver()

    private suspend fun open() = SqliteAddressRepository.open(driver, TestDb.path)

    @Test fun closeWithNoQueryRunningClosesTheConnectionAtOnce() = runBlocking(Dispatchers.Default) {
        val repository = open()
        repository.close()
        assertEquals(1, driver.closeCalls.get())
    }

    @Test fun closingTwiceClosesTheConnectionOnce() = runBlocking(Dispatchers.Default) {
        val repository = open()
        repository.close()
        repository.close()
        assertEquals(1, driver.closeCalls.get())
    }

    @Test fun aQueryInFlightFinishesAndTheLastOneClosesTheConnection() = runBlocking(Dispatchers.Default) {
        val repository = open()
        driver.holdNextQuery()
        val running = async { repository.regions() }
        assertTrue(driver.queryEntered.await(10, TimeUnit.SECONDS), "the query never started")

        repository.close()
        assertEquals(0, driver.closeCalls.get(), "must not close under a running query")

        driver.release()
        assertEquals(31, running.await().size, "the running query still returns its rows")
        assertEquals(1, driver.closeCalls.get(), "closed once the query finished")
    }

    @Test fun aQueryStartedAfterCloseFailsWithAClearError() = runBlocking(Dispatchers.Default) {
        val repository = open()
        repository.close()
        val error = assertFailsWith<IllegalStateException> { repository.regions() }
        assertTrue("closed" in error.message.orEmpty(), error.message)
    }

    @Test fun aQueryStartedAfterCloseFailsAtOnceEvenWhileAnotherIsStillRunning() = runBlocking(Dispatchers.Default) {
        val repository = open()
        driver.holdNextQuery()
        val running = async { repository.regions() }
        assertTrue(driver.queryEntered.await(10, TimeUnit.SECONDS), "the query never started")

        repository.close()
        val error = assertFailsWith<IllegalStateException> { repository.districts("01") } // must not wait for the running query
        assertTrue("closed" in error.message.orEmpty(), error.message)

        driver.release()
        assertEquals(31, running.await().size)
        assertEquals(1, driver.closeCalls.get())
    }

    @Test fun postcodeValidationNeedsNoDatabaseSoItWorksAfterClose() = runBlocking(Dispatchers.Default) {
        val repository = open()
        val postcode = repository.byPrefix("11").first().ward!!.postcode
        repository.close()
        assertTrue(repository.isValidPostcode(postcode))
    }
}
