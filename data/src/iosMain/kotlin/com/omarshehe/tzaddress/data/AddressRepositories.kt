package com.omarshehe.tzaddress.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/** Create once and share; call `close()` when done. Opens the bundled Tanzanian address database, read from [bundle] (the app must include `tz-address.db` and `tz-address.db.version`); the working copy lives in the app's Application Support directory. */
public suspend fun createAddressRepository(bundle: NSBundle = NSBundle.mainBundle): AddressStore {
    val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    return openBundled("$support/tz-address", BundledSQLiteDriver()) { readBundledResource(bundle, it) }
}
