package com.omarshehe.tzaddress.data

/** Create once and share; call `close()` when done. Opens the bundled Tanzanian address database, keeping its working copy in [directory]. */
public suspend fun createAddressRepository(directory: String): AddressStore =
    openBundled(directory, JdbcSQLiteDriver(), ::readBundledResource)
