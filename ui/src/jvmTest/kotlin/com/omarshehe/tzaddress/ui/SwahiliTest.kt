package com.omarshehe.tzaddress.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SwahiliTest {
    private val original = Locale.getDefault()

    @AfterTest fun restoreLocale() = Locale.setDefault(original)

    @Test fun pickerLabelsFollowTheDeviceLanguage() = runComposeUiTest {
        Locale.setDefault(Locale.forLanguageTag("sw"))
        setContent { AddressPicker(FakeRepository(), value = null, onValueChange = {}) }
        waitUntil { onAllNodesWithText("Mkoa").fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText("Wilaya").assertCountEquals(1)
        onAllNodesWithText("Kata (si lazima)").assertCountEquals(0) // Ward is required by default
        onAllNodesWithText("Mtaa / Kijiji (si lazima)").assertCountEquals(1)
    }
}
