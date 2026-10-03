package com.omarshehe.tzaddress.data

import androidx.sqlite.SQLiteDriver

/** [readResource] returns the bytes of a file bundled with the library, per platform. Installs the bundled database under [directory] (copy once, replace on a dataset change) and opens it. */
internal suspend fun openBundled(
    directory: String,
    driver: SQLiteDriver,
    readResource: suspend (name: String) -> ByteArray,
): AddressStore {
    val installer = DbInstaller(
        directory = directory,
        driver = driver,
        bundledStamp = { readResource(DbInstaller.STAMP_RESOURCE).decodeToString() },
        bundledBytes = { readResource(DbInstaller.FILE_NAME) },
    )
    return SqliteAddressRepository.open(driver, installer.install())
}
