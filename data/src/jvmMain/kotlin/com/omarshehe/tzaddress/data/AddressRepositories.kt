package com.omarshehe.tzaddress.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.omarshehe.tzaddress.AddressRepository

/** Opens the bundled Tanzanian address database, keeping its working copy in [directory]. */
public suspend fun createAddressRepository(directory: String): AddressRepository =
    openBundled(directory, BundledSQLiteDriver(), ::readBundledResource)
