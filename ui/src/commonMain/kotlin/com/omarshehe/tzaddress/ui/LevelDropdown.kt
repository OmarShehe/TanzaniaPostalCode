package com.omarshehe.tzaddress.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.ui.resources.Res
import com.omarshehe.tzaddress.ui.resources.picker_none_listed
import com.omarshehe.tzaddress.ui.resources.picker_optional
import com.omarshehe.tzaddress.ui.resources.picker_select
import com.omarshehe.tzaddress.ui.resources.picker_select_first
import org.jetbrains.compose.resources.stringResource

/** One read-only dropdown of the cascade. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LevelDropdown(
    state: PickerLevelState,
    parent: Level?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val name = stringResource(state.level.labelResource())
    val label = if (state.optional) stringResource(Res.string.picker_optional, name) else name
    val selectedLabel = state.options.firstOrNull { it.id == state.selectedId }?.label.orEmpty()
    val support = when {
        state.noneListed -> stringResource(Res.string.picker_none_listed)
        !state.enabled && parent != null -> stringResource(Res.string.picker_select_first, stringResource(parent.labelResource()))
        else -> null
    }

    ExposedDropdownMenuBox(
        expanded = expanded && state.enabled,
        onExpandedChange = { expanded = it && state.enabled },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            enabled = state.enabled,
            label = { Text(label) },
            placeholder = { Text(stringResource(Res.string.picker_select)) },
            supportingText = support?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && state.enabled) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = state.enabled)
                .fillMaxWidth()
                .testTag(levelTag(state.level)),
        )
        ExposedDropdownMenu(expanded = expanded && state.enabled, onDismissRequest = { expanded = false }) {
            state.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onSelect(option.id)
                    },
                )
            }
        }
    }
}

internal fun levelTag(level: Level): String = "address-level-${level.name.lowercase()}"
