package com.omarshehe.tzaddress.data

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.omarshehe.tzaddress.AddressRepository
import java.io.File

/** Opens the bundled Tanzanian address database; the working copy lives in the app's private files directory. */
public suspend fun createAddressRepository(context: Context): AddressRepository =
    openBundled(File(context.filesDir, "tz-address").path, BundledSQLiteDriver(), ::readBundledResource)
