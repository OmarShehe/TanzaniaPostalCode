package com.omarshehe.tzaddress.ui

import com.omarshehe.tzaddress.AddressPath
import com.omarshehe.tzaddress.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class AddressPickerControllerTest {
    private val repo = FakeRepository()
    private val emitted = mutableListOf<AddressPath?>()

    private fun TestScope.controller(required: Level = Level.WARD): AddressPickerController {
        val c = AddressPickerController(repo, backgroundScope, required) { emitted += it }
        runCurrent()
        return c
    }

    private fun AddressPickerController.level(level: Level) = state.value.levels.first { it.level == level }

    private fun TestScope.pick(c: AddressPickerController, level: Level, id: String?) {
        c.select(level, id)
        runCurrent()
    }

    @Test fun initially_onlyRegionIsEnabledAndPopulated() = runTest {
        val c = controller()
        assertTrue(c.level(Level.REGION).enabled)
        assertEquals(listOf("Dar es Salaam", "Tanga"), c.level(Level.REGION).options.map { it.label })
        Level.entries.drop(1).forEach { assertFalse(c.level(it).enabled, "$it") }
        assertTrue(emitted.isEmpty())
    }

    @Test fun choosingRegionEnablesAndFillsDistrict() = runTest {
        val c = controller()
        pick(c, Level.REGION, "11000")
        assertTrue(c.level(Level.DISTRICT).enabled)
        assertEquals(listOf("Ilala CBD", "Temeke"), c.level(Level.DISTRICT).options.map { it.label })
        assertFalse(c.level(Level.WARD).enabled)
    }

    @Test fun changingAParentClearsAllDescendantsAndDisablesThem() = runTest {
        val c = controller()
        pick(c, Level.REGION, "11000"); pick(c, Level.DISTRICT, "11"); pick(c, Level.WARD, "11101"); pick(c, Level.MTAA, repo.kivukoniMtaa.id)
        pick(c, Level.REGION, "21000")
        assertEquals("21000", c.level(Level.REGION).selectedId)
        listOf(Level.DISTRICT, Level.WARD, Level.MTAA, Level.KITONGOJI).forEach {
            assertNull(c.level(it).selectedId, "$it")
        }
        assertFalse(c.level(Level.WARD).enabled)
        assertNull(c.state.value.postcode)
    }

    @Test fun clearingARegionResetsTheRest() = runTest {
        val c = controller()
        pick(c, Level.REGION, "11000"); pick(c, Level.DISTRICT, "11")
        pick(c, Level.REGION, null)
        assertNull(c.level(Level.REGION).selectedId)
        assertFalse(c.level(Level.DISTRICT).enabled)
        assertTrue(c.level(Level.DISTRICT).options.isEmpty())
    }

    @Test fun wardShowsItsFiveDigitPostcode() = runTest {
        val c = controller()
        pick(c, Level.REGION, "11000"); pick(c, Level.DISTRICT, "11"); pick(c, Level.WARD, "11101")
        assertEquals("11101", c.state.value.postcode)
    }

    @Test fun requiredWard_emitsNullUntilWardChosenThenTheWardPath() = runTest {
        val c = controller(Level.WARD)
        pick(c, Level.REGION, "11000"); pick(c, Level.DISTRICT, "11")
        assertTrue(emitted.all { it == null }, "no value before the ward is chosen: $emitted")
        pick(c, Level.WARD, "11101")
        val path = assertNotNull(emitted.last())
        assertEquals("Kivukoni", path.ward?.name)
        assertNull(path.mtaa)
        assertTrue(c.level(Level.MTAA).optional)
        assertTrue(c.level(Level.KITONGOJI).optional)
        assertFalse(c.level(Level.WARD).optional)
    }

    @Test fun deeperChoicesReEmitAndClearingThemFallsBack() = runTest {
        val c = controller(Level.WARD)
        pick(c, Level.REGION, "11000"); pick(c, Level.DISTRICT, "11"); pick(c, Level.WARD, "11101")
        pick(c, Level.MTAA, repo.kivukoniMtaa.id); pick(c, Level.KITONGOJI, repo.seaView.id)
        assertEquals("Sea View", emitted.last()?.kitongoji?.name)
        pick(c, Level.MTAA, null)
        assertNull(emitted.last()?.mtaa)
        assertEquals("Kivukoni", emitted.last()?.ward?.name)
    }

    @Test fun clearingARequiredLevelEmitsNull() = runTest {
        val c = controller(Level.WARD)
        pick(c, Level.REGION, "11000"); pick(c, Level.DISTRICT, "11"); pick(c, Level.WARD, "11101")
        pick(c, Level.DISTRICT, null)
        assertNull(emitted.last())
    }

    @Test fun sameValueIsNotEmittedTwice() = runTest {
        val c = controller(Level.WARD)
        pick(c, Level.REGION, "11000"); pick(c, Level.DISTRICT, "11"); pick(c, Level.WARD, "11101")
        val before = emitted.size
        pick(c, Level.WARD, "11101")
        assertEquals(before, emitted.size)
    }

    @Test fun wardWithoutMtaas_showsNoneListedAndCountsAsCompleteWhenMtaaRequired() = runTest {
        val c = controller(Level.MTAA)
        pick(c, Level.REGION, "11000"); pick(c, Level.DISTRICT, "11"); pick(c, Level.WARD, "11106")
        assertTrue(c.level(Level.MTAA).noneListed)
        assertFalse(c.level(Level.MTAA).enabled)
        assertEquals("Kariakoo", emitted.last()?.ward?.name)
    }

    @Test fun requiredMtaa_isNullUntilMtaaChosen() = runTest {
        val c = controller(Level.MTAA)
        pick(c, Level.REGION, "11000"); pick(c, Level.DISTRICT, "11"); pick(c, Level.WARD, "11101")
        assertNull(emitted.lastOrNull(), "still incomplete, nothing emitted")
        assertNull(c.currentValue)
        pick(c, Level.MTAA, repo.ferryMtaa.id)
        assertEquals("Ferry", emitted.last()?.mtaa?.name)
    }

    @Test fun mtaaWithoutKitongoji_showsNoneListedAndCompletesWhenKitongojiRequired() = runTest {
        val c = controller(Level.KITONGOJI)
        pick(c, Level.REGION, "11000"); pick(c, Level.DISTRICT, "11"); pick(c, Level.WARD, "11101"); pick(c, Level.MTAA, repo.ferryMtaa.id)
        assertTrue(c.level(Level.KITONGOJI).noneListed)
        assertEquals("Ferry", emitted.last()?.mtaa?.name)
    }

    @Test fun requiredKitongoji_wardWithoutMtaasCascadesNoneListed() = runTest {
        val c = controller(Level.KITONGOJI)
        pick(c, Level.REGION, "11000"); pick(c, Level.DISTRICT, "11"); pick(c, Level.WARD, "11106")
        assertTrue(c.level(Level.MTAA).noneListed)
        assertTrue(c.level(Level.KITONGOJI).noneListed)
        assertEquals("Kariakoo", emitted.last()?.ward?.name)
    }

    @Test fun restore_setsEveryLevelWithoutEmitting() = runTest {
        val c = controller()
        val path = assertNotNull(repo.path(Level.KITONGOJI, repo.seaView.id))
        c.restore(path)
        assertEquals("11000", c.level(Level.REGION).selectedId)
        assertEquals("11", c.level(Level.DISTRICT).selectedId)
        assertEquals("11101", c.level(Level.WARD).selectedId)
        assertEquals(repo.kivukoniMtaa.id, c.level(Level.MTAA).selectedId)
        assertEquals(repo.seaView.id, c.level(Level.KITONGOJI).selectedId)
        assertEquals("11101", c.state.value.postcode)
        assertTrue(emitted.isEmpty(), "restoring is not a user change")
        assertEquals(path, c.currentValue)
    }

    @Test fun restoreNull_resetsToTheInitialState() = runTest {
        val c = controller()
        pick(c, Level.REGION, "11000"); pick(c, Level.DISTRICT, "11")
        c.restore(null)
        assertNull(c.level(Level.REGION).selectedId)
        assertFalse(c.level(Level.DISTRICT).enabled)
        assertNull(c.currentValue)
    }

    @Test fun browseFailureSetsAnErrorAndDoesNotCrash() = runTest {
        repo.failBrowse = true
        val c = controller()
        assertNotNull(c.state.value.error)
        assertFalse(c.level(Level.REGION).enabled)
    }

    @Test fun overlappingRestoresEndInTheLastRequestedState() = runTest {
        val c = controller()
        repo.browseDelay = 100
        val path = assertNotNull(repo.path(Level.KITONGOJI, repo.seaView.id))
        backgroundScope.launch { c.restore(path) }
        backgroundScope.launch { c.restore(null) }
        advanceTimeBy(5_000); runCurrent()
        Level.entries.forEach { assertNull(c.level(it).selectedId, "$it must be empty after restore(null)") }
        assertNull(c.currentValue)
    }
}
