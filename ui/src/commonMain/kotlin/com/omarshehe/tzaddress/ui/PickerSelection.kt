package com.omarshehe.tzaddress.ui

import androidx.compose.runtime.saveable.Saver
import com.omarshehe.tzaddress.AddressPath
import com.omarshehe.tzaddress.AddressRepository
import com.omarshehe.tzaddress.Level

/** What survives rotation or process death: only the deepest node's level and id; the chain is rebuilt from the repository. */
internal data class PickerSelection(val level: Level, val id: String)

internal fun AddressPath.toSelection(): PickerSelection = when {
    kitongoji != null -> PickerSelection(Level.KITONGOJI, kitongoji!!.id)
    mtaa != null -> PickerSelection(Level.MTAA, mtaa!!.id)
    ward != null -> PickerSelection(Level.WARD, ward!!.postcode)
    district != null -> PickerSelection(Level.DISTRICT, district!!.code)
    else -> PickerSelection(Level.REGION, region.code)
}

internal suspend fun PickerSelection.resolve(repository: AddressRepository): AddressPath? = repository.path(level, id)

internal val PickerSelectionSaver: Saver<PickerSelection?, Any> = Saver(
    save = { selection -> if (selection == null) emptyList<String>() else listOf(selection.level.name, selection.id) },
    restore = { saved ->
        val parts = (saved as? List<*>)?.filterIsInstance<String>().orEmpty()
        val level = parts.getOrNull(0)?.let { name -> Level.entries.firstOrNull { it.name == name } }
        val id = parts.getOrNull(1)
        if (level != null && id != null) PickerSelection(level, id) else null
    },
)
