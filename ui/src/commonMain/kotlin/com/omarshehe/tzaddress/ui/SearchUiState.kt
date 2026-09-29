package com.omarshehe.tzaddress.ui

import com.omarshehe.tzaddress.AddressMatch

internal sealed interface SearchUiState {
    data object Idle : SearchUiState
    data class Results(val matches: List<AddressMatch>) : SearchUiState
    data object NoMatches : SearchUiState
    data class Error(val message: String?) : SearchUiState
}
