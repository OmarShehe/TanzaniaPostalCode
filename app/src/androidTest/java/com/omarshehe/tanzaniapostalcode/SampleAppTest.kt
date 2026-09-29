package com.omarshehe.tanzaniapostalcode

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** End to end on a device: both widgets over the real bundled database. */
@RunWith(AndroidJUnit4::class)
class SampleAppTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) =
        rule.waitUntil(15_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun pick(field: String, option: String) {
        waitForText(field)
        rule.onAllNodesWithText(field).onFirst().performClick()
        waitForText(option)
        rule.onAllNodesWithText(option).onLast().performClick()
    }

    @Test
    fun searchFindsKivukoniWithDistrictRegionAndPostcode() {
        waitForText("Search address")
        rule.onNode(hasSetTextAction() and hasText("Search address")).performTextInput("kiv")
        waitForText("Kivukoni — Ilala CBD, Dar es Salaam")
        rule.onAllNodesWithText("11101").fetchSemanticsNodes().isNotEmpty().also { check(it) { "postcode not shown" } }
        rule.onNodeWithText("Kivukoni — Ilala CBD, Dar es Salaam").performClick()
        waitForText("Kivukoni (11101), Ilala CBD, Dar es Salaam")
    }

    @Test
    fun pickerCascadeShowsPostcode_andSurvivesRotation() {
        pick("Region", "Dar es Salaam")
        pick("District", "Ilala CBD")
        pick("Ward", "Kivukoni")
        waitForText("Postcode 11101")

        rule.activityRule.scenario.recreate()

        waitForText("Postcode 11101")
        // The host lost its own state with the activity; the picker must tell it the restored value.
        waitForText("Kivukoni (11101), Ilala CBD, Dar es Salaam")
    }
}
