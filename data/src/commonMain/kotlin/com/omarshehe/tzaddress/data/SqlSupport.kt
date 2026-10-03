package com.omarshehe.tzaddress.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

/** Runs [sql], binding text parameters in order, and maps every row. */
internal fun <T> SQLiteConnection.query(sql: String, vararg args: String, row: (SQLiteStatement) -> T): List<T> =
    prepare(sql).use { statement ->
        args.forEachIndexed { i, arg -> statement.bindText(i + 1, arg) }
        buildList { while (statement.step()) add(row(statement)) }
    }
