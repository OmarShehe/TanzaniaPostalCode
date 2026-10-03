package com.omarshehe.tzaddress.data

import androidx.sqlite.SQLiteDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.write
import kotlin.random.Random

/**
 * Puts the bundled database into app storage: copies it once, and again only when the bundled
 * dataset stamp (`version|sourceEdition|generatedAt`) differs from the installed one or the installed file is unreadable.
 */
internal class DbInstaller(
    private val directory: String,
    private val driver: SQLiteDriver,
    private val bundledStamp: suspend () -> String,
    private val bundledBytes: suspend () -> ByteArray,
) {
    /** Returns the path of the installed, current database. */
    suspend fun install(): String {
        val target = Path(directory, FILE_NAME)
        val expected = bundledStamp().trim()
        if (readInstalledStamp(driver, target.toString()) == expected) return target.toString()
        val bytes = bundledBytes()
        return withContext(Dispatchers.Default) {
            SystemFileSystem.createDirectories(Path(directory))
            // Write beside the target and rename, so a crash never leaves a half-written database in place.
            // The name is unique, so two installs at the same moment cannot write the same file.
            val temp = Path(directory, "$FILE_NAME.${Random.nextLong().toULong().toString(16)}.tmp")
            try {
                SystemFileSystem.sink(temp).buffered().use { it.write(bytes) }
                SystemFileSystem.atomicMove(temp, target)
            } finally {
                SystemFileSystem.delete(temp, mustExist = false)
            }
            target.toString()
        }
    }

    companion object {
        const val FILE_NAME = "tz-address.db"
        const val STAMP_RESOURCE = "tz-address.db.version"

        /** Stamp stored in [path]'s `dataset_info`, or null when the file is missing or not a valid database. */
        suspend fun readInstalledStamp(driver: SQLiteDriver, path: String): String? = withContext(Dispatchers.Default) {
            if (!SystemFileSystem.exists(Path(path))) return@withContext null
            try {
                val connection = driver.open(path)
                try {
                    connection.query("SELECT version || '|' || source_edition || '|' || generated_at FROM dataset_info") { it.getText(0) }.singleOrNull()
                } finally {
                    connection.close()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }
}
