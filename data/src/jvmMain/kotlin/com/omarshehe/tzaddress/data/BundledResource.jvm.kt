package com.omarshehe.tzaddress.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun readBundledResource(name: String): ByteArray = withContext(Dispatchers.IO) {
    val stream = SqliteAddressRepository::class.java.classLoader?.getResourceAsStream(name)
        ?: error("Bundled resource '$name' is missing; run :data:generateAddressDb")
    stream.use { it.readBytes() }
}
