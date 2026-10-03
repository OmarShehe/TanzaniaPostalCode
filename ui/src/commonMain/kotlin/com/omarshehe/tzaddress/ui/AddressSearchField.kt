package com.omarshehe.tzaddress.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.omarshehe.tzaddress.AddressPath
import com.omarshehe.forminput.compose.ui.FormInputTextField
import com.omarshehe.forminput.compose.ui.model.FormInputTextFieldState
import com.omarshehe.forminput.compose.ui.model.FormInputType
import com.omarshehe.tzaddress.AddressRepository
import com.omarshehe.tzaddress.ui.resources.Res
import com.omarshehe.tzaddress.ui.resources.search_error
import com.omarshehe.tzaddress.ui.resources.search_label
import com.omarshehe.tzaddress.ui.resources.search_no_matches
import com.omarshehe.tzaddress.ui.resources.search_placeholder
import org.jetbrains.compose.resources.stringResource

/**
 * Type-ahead address search. Suggestions appear under the field after a short pause in typing;
 * choosing one fills the field and calls [onSelected].
 */
@Composable
public fun AddressSearchField(
    repository: AddressRepository,
    onSelected: (AddressPath) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.search_label),
    placeholder: String = stringResource(Res.string.search_placeholder),
) {
    val scope = rememberCoroutineScope()
    val controller = remember(repository) { AddressSearchController(repository, scope) }
    val state by controller.state.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    var suggestionsVisible by rememberSaveable { mutableStateOf(true) }

    Column(modifier) {
        FormInputTextField(
            state = FormInputTextFieldState(
                id = "address-search",
                type = FormInputType.TEXT,
                value = text,
                label = label,
                placeholder = placeholder,
            ),
            textModifier = Modifier.testTag(SEARCH_FIELD_TAG),
            onValueChange = {
                text = it.value
                suggestionsVisible = true
                controller.onQueryChange(it.value)
            },
        )
        if (suggestionsVisible) {
            when (val current = state) {
                SearchUiState.Idle -> Unit
                SearchUiState.NoMatches -> InfoRow(stringResource(Res.string.search_no_matches), error = false)
                is SearchUiState.Error -> InfoRow(stringResource(Res.string.search_error), error = true)
                is SearchUiState.Results -> current.matches.forEach { match ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                text = match.label
                                suggestionsVisible = false
                                onSelected(match.path)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(match.suggestionTitle(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        match.postcode?.let {
                            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(message: String, error: Boolean) {
    Text(
        message,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

internal const val SEARCH_FIELD_TAG = "address-search-field"
