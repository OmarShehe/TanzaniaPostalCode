package com.omarshehe.tzaddress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.omarshehe.tzaddress.AddressPath
import com.omarshehe.tzaddress.AddressRepository
import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.ui.resources.Res
import com.omarshehe.tzaddress.ui.resources.picker_error
import com.omarshehe.tzaddress.ui.resources.picker_postcode
import org.jetbrains.compose.resources.stringResource

/**
 * Cascading Region → District → Ward → Mtaa/Village → Kitongoji picker.
 *
 * [onValueChange] receives null until every level up to [requiredLevel] is chosen (or has nothing listed),
 * then the deepest chosen [AddressPath]. Pass a new [value] (for example null to reset the form) to change the selection from outside.
 * The selection survives rotation and process death on Android.
 */
@Composable
public fun AddressPicker(
    repository: AddressRepository,
    value: AddressPath?,
    onValueChange: (AddressPath?) -> Unit,
    modifier: Modifier = Modifier,
    requiredLevel: Level = Level.WARD,
) {
    val scope = rememberCoroutineScope()
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    var saved by rememberSaveable(stateSaver = PickerSelectionSaver) { mutableStateOf<PickerSelection?>(value?.toSelection()) }
    val initialSelection = remember { saved }
    val controller = remember(repository, requiredLevel) {
        AddressPickerController(repository, scope, requiredLevel) { currentOnValueChange(it) }
    }
    val state by controller.state.collectAsState()

    LaunchedEffect(controller) {
        controller.restore(value ?: initialSelection?.resolve(repository))
        // A selection restored from the picker's own saved state is news to a host that lost its state (e.g. after rotation).
        if (controller.currentValue != value) currentOnValueChange(controller.currentValue)
    }
    // Keep even a partial selection (say, only a region) across rotation.
    LaunchedEffect(controller) {
        controller.state.collect { saved = controller.selectionPath?.toSelection() }
    }
    // Follow the host when it changes the value (e.g. resets the form); ignore echoes of our own emissions.
    var lastSeenValue by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value != lastSeenValue) {
            lastSeenValue = value
            if (value != controller.currentValue) controller.restore(value)
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.error?.let {
            Text(stringResource(Res.string.picker_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        state.levels.forEach { level ->
            LevelDropdown(
                state = level,
                parent = Level.entries.getOrNull(level.level.ordinal - 1),
                onSelect = { controller.select(level.level, it) },
            )
        }
        state.postcode?.let {
            Text(
                stringResource(Res.string.picker_postcode, it),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp).testTag(POSTCODE_TAG),
            )
        }
    }
}

internal const val POSTCODE_TAG = "address-postcode"
