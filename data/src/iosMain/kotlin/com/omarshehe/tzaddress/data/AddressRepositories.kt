package com.omarshehe.tzaddress.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.omarshehe.tzaddress.model.ExtraPlace
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/** Create once and share; call `close()` when done. Opens the bundled Tanzanian address database, read from [bundle] (the app must include `tz-address.db` and `tz-address.db.version`); the working copy lives in the app's Application Support directory. */
public suspend fun createAddressRepository(bundle: NSBundle = NSBundle.mainBundle): AddressStore {
    val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    return openBundled("$support/tz-address", BundledSQLiteDriver(), readResource = { readBundledResource(bundle, it) })
}

/** As above, with [extraPlaces] (wards, mtaa/villages, kitongoji) served beside the bundled ones; throws [ExtraPlacesInvalidException] listing every problem. */
public suspend fun createAddressRepository(bundle: NSBundle = NSBundle.mainBundle, extraPlaces: List<ExtraPlace>): AddressStore {
    val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    return openBundled("$support/tz-address", BundledSQLiteDriver(), readResource = { readBundledResource(bundle, it) }, extraPlaces = extraPlaces)
}
