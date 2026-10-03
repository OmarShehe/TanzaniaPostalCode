package com.omarshehe.tzaddress.data

import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.Properties
import org.sqlite.JDBC

/** The JVM `androidx.sqlite` driver: sqlite-jdbc, which ships native SQLite for macOS (Intel and Apple silicon), Windows, Linux and Alpine. */
internal class JdbcSQLiteDriver : SQLiteDriver {
    override val hasConnectionPool: Boolean = false

    override fun open(fileName: String): SQLiteConnection {
        // Straight to the sqlite-jdbc driver: no DriverManager registry (fat jars and custom classloaders can miss it), and the file name is passed as a path, not parsed as URL parameters.
        val connection = JDBC.createConnection("${JDBC.PREFIX}${databaseUri(fileName)}", Properties())
        return JdbcConnection(connection)
    }

    /** A plain path unless it holds `?`, `%` or `#`: then a `file:` URI with those escaped, so URL characters in a folder name are not read as parameters. */
    private fun databaseUri(fileName: String): String =
        if (fileName.none { it == '?' || it == '%' || it == '#' }) fileName
        else "file:" + fileName.replace('\\', '/').replace("%", "%25").replace("?", "%3F").replace("#", "%23")

    private class JdbcConnection(private val connection: Connection) : SQLiteConnection {
        override fun inTransaction(): Boolean = !connection.autoCommit
        override fun prepare(sql: String): SQLiteStatement = JdbcStatement(connection.prepareStatement(sql))
        override fun close() = connection.close()
    }

    private class JdbcStatement(private val statement: PreparedStatement) : SQLiteStatement {
        private var results: ResultSet? = null
        private var stepped = false

        override fun bindBlob(index: Int, value: ByteArray) = statement.setBytes(index, value)
        override fun bindDouble(index: Int, value: Double) = statement.setDouble(index, value)
        override fun bindLong(index: Int, value: Long) = statement.setLong(index, value)
        override fun bindText(index: Int, value: String) = statement.setString(index, value)
        override fun bindNull(index: Int) = statement.setNull(index, Types.NULL)

        // Column indexes are 0-based in androidx.sqlite and 1-based in JDBC.
        override fun getBlob(index: Int): ByteArray = rs().getBytes(index + 1) ?: ByteArray(0)
        override fun getDouble(index: Int): Double = rs().getDouble(index + 1)
        override fun getLong(index: Int): Long = rs().getLong(index + 1)
        override fun getText(index: Int): String = rs().getString(index + 1) ?: "" // a NULL reads as empty, as in androidx.sqlite
        override fun isNull(index: Int): Boolean { rs().getObject(index + 1); return rs().wasNull() }
        override fun getColumnCount(): Int = statement.metaData?.columnCount ?: 0
        override fun getColumnName(index: Int): String = statement.metaData.getColumnName(index + 1)
        override fun getColumnType(index: Int): Int = when (rs().getObject(index + 1)) {
            null -> SQLITE_DATA_NULL
            is Long, is Int -> SQLITE_DATA_INTEGER
            is Double, is Float -> SQLITE_DATA_FLOAT
            is ByteArray -> SQLITE_DATA_BLOB
            else -> SQLITE_DATA_TEXT
        }

        override fun step(): Boolean {
            if (!stepped) {
                stepped = true
                if (statement.execute()) results = statement.resultSet else return false
            }
            return results?.next() ?: false
        }

        override fun reset() { results?.close(); results = null; stepped = false }
        override fun clearBindings() = statement.clearParameters()
        override fun close() { results?.close(); statement.close() }
        private fun rs(): ResultSet = checkNotNull(results) { "no current row" }
    }
}
