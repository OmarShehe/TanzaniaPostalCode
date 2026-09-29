package com.omarshehe.tzaddress.ui

import com.omarshehe.tzaddress.AddressRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

/** Debounced type-ahead: a newer query cancels the running search, failures become [SearchUiState.Error]. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal class AddressSearchController(
    private val repository: AddressRepository,
    scope: CoroutineScope,
    debounceMillis: Long = DEBOUNCE_MILLIS,
    private val maxSuggestions: Int = MAX_SUGGESTIONS,
) {
    private val query = MutableStateFlow("")
    private val mutableState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = mutableState

    init {
        query
            .debounce(debounceMillis)
            .distinctUntilChanged()
            // Blank goes through mapLatest too, so clearing the field cancels a search that is still running.
            .mapLatest { if (it.isBlank()) SearchUiState.Idle else search(it) }
            .onEach { mutableState.value = it }
            .launchIn(scope)
    }

    fun onQueryChange(text: String) {
        query.value = text
        // Blank input clears at once instead of waiting for the debounce.
        if (text.isBlank()) mutableState.value = SearchUiState.Idle
    }

    private suspend fun search(text: String): SearchUiState =
        try {
            val matches = repository.search(text, limit = maxSuggestions).take(maxSuggestions)
            if (matches.isEmpty()) SearchUiState.NoMatches else SearchUiState.Results(matches)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SearchUiState.Error(e.message)
        }

    companion object {
        const val DEBOUNCE_MILLIS = 250L
        const val MAX_SUGGESTIONS = 10
    }
}
