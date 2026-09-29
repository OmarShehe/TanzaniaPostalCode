package com.omarshehe.tzaddress.ui

internal data class PickerUiState(
    val levels: List<PickerLevelState>,
    /** Ward postcode once a ward is chosen. */
    val postcode: String?,
    val error: String?,
)
