package com.omarshehe.tzaddress.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.omarshehe.forminput.compose.ui.FormInputDropDownOption
import com.omarshehe.forminput.compose.ui.model.DropDownOptionModel
import com.omarshehe.forminput.compose.ui.model.FormInputDropDownState
import com.omarshehe.forminput.compose.ui.model.FormInputType
import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.ui.resources.Res
import com.omarshehe.tzaddress.ui.resources.picker_none_listed
import com.omarshehe.tzaddress.ui.resources.picker_optional
import com.omarshehe.tzaddress.ui.resources.picker_select
import com.omarshehe.tzaddress.ui.resources.picker_select_first
import org.jetbrains.compose.resources.stringResource

/** One read-only dropdown of the cascade, built on forminput-compose. */
@Composable
internal fun LevelDropdown(
    state: PickerLevelState,
    parent: Level?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(state.level.labelResource())
    val label = if (state.optional) stringResource(Res.string.picker_optional, name) else name
    val selectPlaceholder = stringResource(Res.string.picker_select)
    val support = when {
        state.noneListed -> stringResource(Res.string.picker_none_listed)
        !state.enabled && parent != null -> stringResource(Res.string.picker_select_first, stringResource(parent.labelResource()))
        else -> null
    }

    val data = remember(state.options, state.selectedId, label) {
        FormInputDropDownState(
            value = state.options.firstOrNull { it.id == state.selectedId }
                ?.let { DropDownOptionModel(it.id, it.label) } ?: DropDownOptionModel(),
            options = state.options.map { DropDownOptionModel(it.id, it.label) },
            id = levelTag(state.level),
            labelValue = label,
            placeholderValue = selectPlaceholder,
            type = FormInputType.DROP_DOWN,
        )
    }

    FormInputDropDownOption(
        modifier = modifier.fillMaxWidth(),
        fieldModifier = Modifier.testTag(levelTag(state.level)),
        formInputData = data,
        enabled = state.enabled,
        supportingText = support,
        onSelected = { onSelect(it.value.id) },
    )
}

internal fun levelTag(level: Level): String = "address-level-${level.name.lowercase()}"
