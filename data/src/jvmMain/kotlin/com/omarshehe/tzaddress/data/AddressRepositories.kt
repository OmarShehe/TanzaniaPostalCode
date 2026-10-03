package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.model.ExtraPlace

/** Create once and share; call `close()` when done. Opens the bundled Tanzanian address database, keeping its working copy in [directory]. */
public suspend fun createAddressRepository(directory: String): AddressStore =
    openBundled(directory, JdbcSQLiteDriver(), ::readBundledResource)

/** As above, with [extraPlaces] (wards, mtaa/villages, kitongoji) served beside the bundled ones; throws [ExtraPlacesInvalidException] listing every problem. */
public suspend fun createAddressRepository(directory: String, extraPlaces: List<ExtraPlace>): AddressStore =
    openBundled(directory, JdbcSQLiteDriver(), ::readBundledResource, extraPlaces)
