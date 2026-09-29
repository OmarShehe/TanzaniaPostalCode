package com.omarshehe.tzaddress.data

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

/** Create once and share; call `close()` when done. Opens the bundled Tanzanian address database; the working copy lives in the app's private files directory. */
public suspend fun createAddressRepository(context: Context): AddressStore =
    openBundled(File(context.filesDir, "tz-address").path, BundledSQLiteDriver(), ::readBundledResource)
