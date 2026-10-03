package com.omarshehe.tzaddress.data

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.omarshehe.tzaddress.model.ExtraPlace
import java.io.File

/** Create once and share; call `close()` when done. Opens the bundled Tanzanian address database; the working copy lives in the app's private files directory. */
public suspend fun createAddressRepository(context: Context): AddressStore =
    openBundled(File(context.filesDir, "tz-address").path, BundledSQLiteDriver(), ::readBundledResource)

/** As above, with [extraPlaces] (wards, mtaa/villages, kitongoji) served beside the bundled ones; throws [ExtraPlacesInvalidException] listing every problem. */
public suspend fun createAddressRepository(context: Context, extraPlaces: List<ExtraPlace>): AddressStore =
    openBundled(File(context.filesDir, "tz-address").path, BundledSQLiteDriver(), ::readBundledResource, extraPlaces)
