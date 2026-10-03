package com.omarshehe.tzaddress.data

import androidx.sqlite.SQLiteDriver
import com.omarshehe.tzaddress.model.ExtraPlace

/** [extraPlaces] are layered over the database when not empty. [readResource] returns the bytes of a file bundled with the library, per platform. Installs the bundled database under [directory] (copy once, replace on a dataset change) and opens it. */
internal suspend fun openBundled(
    directory: String,
    driver: SQLiteDriver,
    readResource: suspend (name: String) -> ByteArray,
    extraPlaces: List<ExtraPlace> = emptyList(),
): AddressStore {
    val installer = DbInstaller(
        directory = directory,
        driver = driver,
        bundledStamp = { readResource(DbInstaller.STAMP_RESOURCE).decodeToString() },
        bundledBytes = { readResource(DbInstaller.FILE_NAME) },
    )
    val store = SqliteAddressRepository.open(driver, installer.install())
    if (extraPlaces.isEmpty()) return store
    return LayeredAddressStore.create(store, extraPlaces)
}
