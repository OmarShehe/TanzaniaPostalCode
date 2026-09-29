package com.omarshehe.tzaddress.data

import java.io.File

object TestDb {
    /** Built by `:data:generateAddressDb`, which every test task depends on. */
    val path: String = File("build/generated/addressDb/tz-address.db").absolutePath
}
