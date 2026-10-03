package com.omarshehe.tzaddress.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.omarshehe.tzaddress.AddressPath
import com.omarshehe.tzaddress.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class AddressWidgetsTest {
    private val repo = FakeRepository()

    @Test fun search_typingShowsSuggestionsWithDistrictRegionAndPostcode_andSelectingReportsThePath() = runComposeUiTest {
        repo.searchHandler = { listOf(repo.match(Level.WARD, "11101", "Kivukoni")) }
        var selected: AddressPath? = null
        setContent { AddressSearchField(repo, onSelected = { selected = it }) }

        onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("kiv")
        waitUntil { onAllNodesWithText("Kivukoni — Ilala CBD, Dar es Salaam").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("11101").assertExists()

        onNodeWithText("Kivukoni — Ilala CBD, Dar es Salaam").performClick()
        assertEquals("Kivukoni", selected?.ward?.name)
        assertEquals("Dar es Salaam", selected?.region?.name)
        onAllNodesWithText("Kivukoni — Ilala CBD, Dar es Salaam").assertCountEquals(0)
    }

    @Test fun search_emptyQueryShowsNothing_nonsenseShowsNoMatches() = runComposeUiTest {
        setContent { AddressSearchField(repo, onSelected = {}) }
        onAllNodesWithText("No matches").assertCountEquals(0)
        onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("zzzz")
        waitUntil { onAllNodesWithText("No matches").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun search_throwingRepositoryShowsAnInlineError() = runComposeUiTest {
        repo.searchHandler = { error("db closed") }
        setContent { AddressSearchField(repo, onSelected = {}) }
        onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("kiv")
        waitUntil { onAllNodesWithText("Search failed. Try again.").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun picker_districtIsDisabledUntilRegionIsChosen() = runComposeUiTest {
        setContent { AddressPicker(repo, value = null, onValueChange = {}) }
        waitUntil { onNodeWithTag(levelTag(Level.REGION)).isEnabledNow() }
        onNodeWithTag(levelTag(Level.DISTRICT)).assertIsNotEnabled()
        pick(Level.REGION, "Dar es Salaam")
        waitUntil { onNodeWithTag(levelTag(Level.DISTRICT)).isEnabledNow() }
        onNodeWithTag(levelTag(Level.DISTRICT)).assertIsEnabled()
    }

    @Test fun picker_changingRegionClearsTheRest_andWardShowsPostcode() = runComposeUiTest {
        var value: AddressPath? = null
        setContent { AddressPicker(repo, value = value, onValueChange = { value = it }) }
        waitUntil { onNodeWithTag(levelTag(Level.REGION)).isEnabledNow() }
        pick(Level.REGION, "Dar es Salaam")
        waitUntil { onNodeWithTag(levelTag(Level.DISTRICT)).isEnabledNow() }
        pick(Level.DISTRICT, "Ilala CBD")
        waitUntil { onNodeWithTag(levelTag(Level.WARD)).isEnabledNow() }
        pick(Level.WARD, "Kivukoni")
        waitUntil { onAllNodesWithText("Postcode 11101").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("Kivukoni", assertNotNull(value).ward?.name)

        pick(Level.REGION, "Tanga")
        waitUntil { onAllNodesWithText("Postcode 11101").fetchSemanticsNodes().isEmpty() }
        onNodeWithTag(levelTag(Level.WARD)).assertIsNotEnabled()
        assertNull(value)
    }

    @Test fun picker_hostResettingTheValueClearsTheSelection() = runComposeUiTest {
        var value by mutableStateOf<AddressPath?>(null)
        setContent { AddressPicker(repo, value = value, onValueChange = { value = it }) }
        waitUntil { onNodeWithTag(levelTag(Level.REGION)).isEnabledNow() }
        pick(Level.REGION, "Dar es Salaam")
        waitUntil { onNodeWithTag(levelTag(Level.DISTRICT)).isEnabledNow() }
        pick(Level.DISTRICT, "Ilala CBD")
        waitUntil { onNodeWithTag(levelTag(Level.WARD)).isEnabledNow() }
        pick(Level.WARD, "Kivukoni")
        waitUntil { value != null }
        waitForIdle() // let the picker see the ward before the host clears it
        value = null
        waitUntil { onNodeWithTag(levelTag(Level.DISTRICT)).isNotEnabledNow() }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.pick(level: Level, optionLabel: String) {
        onNodeWithTag(levelTag(level)).performClick()
        waitUntil { onAllNodesWithText(optionLabel).fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText(optionLabel).onLast().performClick()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.isEnabledNow(): Boolean =
        runCatching { assertIsEnabled() }.isSuccess

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.isNotEnabledNow(): Boolean =
        runCatching { assertIsNotEnabled() }.isSuccess
}
