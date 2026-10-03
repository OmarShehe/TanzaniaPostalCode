package com.omarshehe.tzaddress.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.execSQL
import com.omarshehe.tzaddress.AddressMatch
import com.omarshehe.tzaddress.AddressPath
import com.omarshehe.tzaddress.AddressText
import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.model.DatasetInfo
import com.omarshehe.tzaddress.model.District
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import com.omarshehe.tzaddress.model.Region
import com.omarshehe.tzaddress.model.Ward
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile

/**
 * Read-only repository over the bundled database. One connection, serialised by a mutex, used off the caller's thread.
 *
 * [close] never blocks and never closes the connection under a running query: with a query in flight it only records the
 * request, and the last query to finish closes the connection. Queries that start after [close] fail with an
 * [IllegalStateException]. Closing twice is safe.
 */
internal class SqliteAddressRepository private constructor(
    private val connection: SQLiteConnection,
    private val wardPostcodes: Set<String>,
) : AddressStore {
    private val lock = Mutex()

    /** Set by [close]; read after every query to see whether this one must close the connection. */
    @Volatile
    private var closeRequested = false

    /** Only written while holding [lock]. */
    private var closed = false

    private suspend fun <T> read(block: (SQLiteConnection) -> T): T =
        withContext(Dispatchers.Default) {
            // A query that starts after close() fails at once instead of queueing behind the one still running.
            check(!closeRequested) { CLOSED_MESSAGE }
            try {
                lock.withLock {
                    // A query that queued before close() but gets the lock after the connection closed.
                    check(!closed) { CLOSED_MESSAGE }
                    block(connection)
                }
            } finally {
                // Checked after the unlock: a close() that found the lock taken relies on this to run its request.
                closeIfRequested()
            }
        }

    override fun close() {
        closeRequested = true
        closeIfRequested()
    }

    /** Closes now when a close was requested and no query holds the lock; otherwise the query that does will call this when it ends. */
    private fun closeIfRequested() {
        if (!closeRequested || !lock.tryLock()) return
        try {
            if (!closed) {
                closed = true
                connection.close()
            }
        } finally {
            lock.unlock()
        }
    }

    override suspend fun info(): DatasetInfo = read { c ->
        c.query("SELECT version, source_edition, generated_at, attribution FROM dataset_info") {
            DatasetInfo(it.getText(0), it.getText(1), it.getText(2), it.getText(3))
        }.single()
    }

    override suspend fun regions(): List<Region> = read { c ->
        c.query("SELECT code, name FROM region ORDER BY name COLLATE NOCASE, code") { Region(it.getText(0), it.getText(1)) }
    }

    override suspend fun districts(regionCode: String): List<District> = read { c ->
        c.query("SELECT code, name, region_code FROM district WHERE region_code = ? ORDER BY name COLLATE NOCASE, code", regionCode) {
            District(it.getText(0), it.getText(1), it.getText(2))
        }
    }

    override suspend fun wards(districtCode: String): List<Ward> = read { c ->
        c.query("SELECT postcode, name, district_code, latitude, longitude FROM ward WHERE district_code = ? ORDER BY name COLLATE NOCASE, postcode", districtCode) {
            Ward(it.getText(0), it.getText(1), it.getText(2), it.getNullableDouble(3), it.getNullableDouble(4))
        }
    }

    override suspend fun mtaas(wardPostcode: String): List<Mtaa> = read { c ->
        c.query("SELECT id, name, ward_postcode FROM mtaa WHERE ward_postcode = ? ORDER BY name COLLATE NOCASE, id", wardPostcode) {
            Mtaa(it.getText(0), it.getText(1), it.getText(2))
        }
    }

    override suspend fun kitongojis(mtaaId: String): List<Kitongoji> = read { c ->
        c.query("SELECT id, name, mtaa_id FROM kitongoji WHERE mtaa_id = ? ORDER BY name COLLATE NOCASE, id", mtaaId) {
            Kitongoji(it.getText(0), it.getText(1), it.getText(2))
        }
    }

    override suspend fun search(query: String, limit: Int, levels: Set<Level>): List<AddressMatch> {
        val match = SearchQuery.match(query) ?: return emptyList()
        if (levels.isEmpty()) return emptyList()
        val max = SearchQuery.clampLimit(limit)
        val normalized = AddressText.normalize(query)
        val levelList = levels.joinToString(",") { "'${it.name}'" }
        return read { c ->
            val candidates = c.query(
                "SELECT level, ref_id, name FROM search_index WHERE search_index MATCH ? AND level IN ($levelList) " +
                    // Exact and prefix names first, so they can't be cut off by the candidate cap; names are stored normalised.
                    "ORDER BY (name = ?) DESC, (name LIKE ? || '%') DESC, bm25(search_index, 0.0, 0.0, 10.0, 1.0) LIMIT $CANDIDATES",
                match, normalized, normalized,
            ) { Triple(Level.valueOf(it.getText(0)), it.getText(1), it.getText(2)) }
            candidates
                .map { (level, id, name) -> SearchRanking.Ranked(SearchRanking.score(query, name), level, name, id) }
                .sortedWith(SearchRanking.order)
                .take(max)
                .mapNotNull { ranked ->
                    pathOf(c, ranked.level, ranked.refId)?.let { path ->
                        AddressMatch(path, ranked.level, PathQueries.label(ranked.level, path), path.ward?.postcode, ranked.score)
                    }
                }
        }
    }

    override suspend fun byPostcode(postcode: String): AddressPath? =
        if (!isValidPostcode(postcode)) null else path(Level.WARD, postcode)

    override suspend fun byPrefix(prefix: String): List<AddressPath> {
        if (prefix.length !in PREFIX_LENGTHS || !prefix.all { it in '0'..'9' }) return emptyList()
        return read { c ->
            c.query("SELECT postcode FROM ward WHERE postcode >= ? AND postcode < ? ORDER BY postcode", prefix, prefix + "￿") { it.getText(0) }
                .mapNotNull { pathOf(c, Level.WARD, it) }
        }
    }

    override suspend fun path(level: Level, id: String): AddressPath? = read { pathOf(it, level, id) }

    override fun isValidPostcode(postcode: String): Boolean = postcode in wardPostcodes

    private fun pathOf(c: SQLiteConnection, level: Level, id: String): AddressPath? =
        c.query(PathQueries.sql(level), id) { PathQueries.read(level, it) }.firstOrNull()

    companion object {
        private const val CLOSED_MESSAGE = "The address store is closed."
        private const val CANDIDATES = 500
        private val PREFIX_LENGTHS = setOf(2, 3, 5)

        /** Opens [path] read-only and preloads the ward postcodes that back [isValidPostcode]. */
        suspend fun open(driver: SQLiteDriver, path: String): SqliteAddressRepository = withContext(Dispatchers.Default) {
            // The common driver API has no read-only open flag; query_only makes every write fail instead.
            val connection = driver.open(path).also { it.execSQL("PRAGMA query_only = ON") }
            val postcodes = connection.query("SELECT postcode FROM ward") { it.getText(0) }.toHashSet()
            SqliteAddressRepository(connection, postcodes)
        }
    }
}
