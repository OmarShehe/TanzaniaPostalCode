package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.model.ExtraPlace
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class ExtraPlacesFactoryTest {

    @Test fun theFactoryServesExtras_andNeverWritesThemToTheBundledDatabase() = runBlocking {
        val dir = createTempDirectory("extras").toFile()
        val database = File(dir, DbInstaller.FILE_NAME)
        createAddressRepository(dir.path).also { it.close() }
        val before = database.readBytes()

        val store = createAddressRepository(dir.path, listOf(ExtraPlace(Level.MTAA, "Soko Jipya", "11101")))
        assertTrue("Soko Jipya" in store.mtaas("11101").map { it.name })
        store.close()

        assertContentEquals(before, database.readBytes())
        val plain = createAddressRepository(dir.path)
        assertEquals(listOf("Kivukoni", "Sea View"), plain.mtaas("11101").map { it.name })
        plain.close()
        dir.deleteRecursively()
        Unit
    }

    @Test fun invalidExtrasFailTheCreationWithEveryProblem() {
        val dir = createTempDirectory("extras").toFile()
        val error = assertFailsWith<ExtraPlacesInvalidException> {
            runBlocking { createAddressRepository(dir.path, listOf(ExtraPlace(Level.MTAA, " ", "99999"), ExtraPlace(Level.MTAA, "Ok", "00000"))) }
        }
        assertEquals(3, error.problems.size, error.problems.toString())
        dir.deleteRecursively()
    }

    @Test fun anEmptyListGivesThePlainStore() = runBlocking {
        val dir = createTempDirectory("extras").toFile()
        val store = createAddressRepository(dir.path, emptyList())
        assertEquals(emptyList(), store.extraPlaceStatuses())
        store.close()
        dir.deleteRecursively()
        Unit
    }
}
