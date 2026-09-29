package com.omarshehe.tzaddress.ui

import androidx.compose.runtime.saveable.SaverScope
import com.omarshehe.tzaddress.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class PickerSelectionTest {
    private val repo = FakeRepository()
    private val scope = SaverScope { true }

    @Test fun deepestLevelWinsForEveryPath() = runTest {
        assertEquals(PickerSelection(Level.REGION, "11000"), assertNotNull(repo.path(Level.REGION, "11000")).toSelection())
        assertEquals(PickerSelection(Level.DISTRICT, "11"), assertNotNull(repo.path(Level.DISTRICT, "11")).toSelection())
        assertEquals(PickerSelection(Level.WARD, "11101"), assertNotNull(repo.path(Level.WARD, "11101")).toSelection())
        assertEquals(PickerSelection(Level.MTAA, repo.ferryMtaa.id), assertNotNull(repo.path(Level.MTAA, repo.ferryMtaa.id)).toSelection())
        assertEquals(PickerSelection(Level.KITONGOJI, repo.seaView.id), assertNotNull(repo.path(Level.KITONGOJI, repo.seaView.id)).toSelection())
    }

    @Test fun resolveRestoresThePathThroughTheRepository() = runTest {
        val selection = PickerSelection(Level.KITONGOJI, repo.seaView.id)
        assertEquals(repo.path(Level.KITONGOJI, repo.seaView.id), selection.resolve(repo))
        assertNull(PickerSelection(Level.WARD, "99999").resolve(repo))
    }

    @Test fun saverRoundTrips() {
        val saver = PickerSelectionSaver
        val saved = with(saver) { scope.save(PickerSelection(Level.WARD, "11101")) }
        assertEquals(PickerSelection(Level.WARD, "11101"), saved?.let(saver::restore))
    }

    @Test fun saverRoundTripsNothingSelected() {
        val saver = PickerSelectionSaver
        val saved = with(saver) { scope.save(null) }
        assertNull(saved?.let(saver::restore))
    }

    @Test fun saverIgnoresCorruptState() {
        assertNull(PickerSelectionSaver.restore(listOf("NOT_A_LEVEL", "1")))
        assertNull(PickerSelectionSaver.restore(listOf("WARD")))
    }
}
