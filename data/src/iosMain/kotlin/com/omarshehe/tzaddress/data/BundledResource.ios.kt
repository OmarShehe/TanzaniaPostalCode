package com.omarshehe.tzaddress.data

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
/** Reads a file from [bundle]; the host app adds `tz-address.db` and `tz-address.db.version` to its target's resources. */
internal fun readBundledResource(bundle: NSBundle, name: String): ByteArray {
    val path = bundle.pathForResource(name.substringBeforeLast('.'), name.substringAfterLast('.'))
        ?: error("Bundled resource '$name' not found in the app bundle; add it to the app target's Copy Bundle Resources")
    val data = NSData.dataWithContentsOfFile(path) ?: error("Cannot read '$path'")
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return bytes
}
