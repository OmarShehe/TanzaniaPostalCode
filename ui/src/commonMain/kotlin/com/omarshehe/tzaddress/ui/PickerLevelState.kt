package com.omarshehe.tzaddress.ui

import com.omarshehe.tzaddress.Level

internal data class PickerLevelState(
    val level: Level,
    val options: List<PickerOption>,
    val selectedId: String?,
    val enabled: Boolean,
    /** Below the form's required level. */
    val optional: Boolean,
    /** The parent is chosen but the data has no children here (a source gap); counts as complete. */
    val noneListed: Boolean,
)
