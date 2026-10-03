package com.omarshehe.tzaddress.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.omarshehe.tzaddress.AddressPath
import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.data.createAddressRepository
import com.omarshehe.tzaddress.model.ExtraPlace
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalTestApi::class)
class ExtraPlacesPickerTest {

    @Test fun anExtraMtaaAppearsInTheCascadeLikeABuiltInOne() = runComposeUiTest {
        val dir = createTempDirectory("picker").toFile()
        val store = runBlocking { createAddressRepository(dir.path, listOf(ExtraPlace(Level.MTAA, "Soko Jipya", "11101"))) }
        var value by mutableStateOf<AddressPath?>(null)
        setContent { AddressPicker(store, value = value, onValueChange = { value = it }) }

        waitUntil { runCatching { onNodeWithTag(levelTag(Level.REGION)).assertIsEnabled() }.isSuccess }
        pick(Level.REGION, "Dar es Salaam")
        waitUntil { runCatching { onNodeWithTag(levelTag(Level.DISTRICT)).assertIsEnabled() }.isSuccess }
        pick(Level.DISTRICT, "Ilala CBD")
        waitUntil { runCatching { onNodeWithTag(levelTag(Level.WARD)).assertIsEnabled() }.isSuccess }
        pick(Level.WARD, "Kivukoni")
        waitUntil { runCatching { onNodeWithTag(levelTag(Level.MTAA)).assertIsEnabled() }.isSuccess }
        pick(Level.MTAA, "Soko Jipya")

        waitUntil { value?.mtaa != null }
        assertEquals("Soko Jipya", assertNotNull(value).mtaa?.name)
        assertEquals("11101", value?.ward?.postcode)
        store.close()
        dir.deleteRecursively()
    }

    private fun androidx.compose.ui.test.ComposeUiTest.pick(level: Level, optionLabel: String) {
        onNodeWithTag(levelTag(level)).performClick()
        waitUntil { onAllNodesWithText(optionLabel).fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText(optionLabel).onLast().performClick()
    }
}
