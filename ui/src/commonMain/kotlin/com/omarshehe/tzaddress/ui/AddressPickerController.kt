package com.omarshehe.tzaddress.ui

import com.omarshehe.tzaddress.AddressPath
import com.omarshehe.tzaddress.AddressRepository
import com.omarshehe.tzaddress.Level
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Cascade rules for the five dependent dropdowns. Not thread-safe: call it from one dispatcher (the UI thread).
 * [onValueChange] gets null until every level up to [requiredLevel] is complete, then the deepest chosen path.
 */
internal class AddressPickerController(
    private val repository: AddressRepository,
    private val scope: CoroutineScope,
    private val requiredLevel: Level,
    private val onValueChange: (AddressPath?) -> Unit,
) {
    private val options = mutableMapOf<Level, List<PickerOption>>()
    private val loaded = mutableSetOf<Level>()
    private val selected = mutableMapOf<Level, PickerOption>()
    private var error: String? = null
    private var lastValue: AddressPath? = null
    private var loadJob: Job? = null

    private val mutableState = MutableStateFlow(snapshot())
    val state: StateFlow<PickerUiState> = mutableState

    /** The path the current selection represents, or null while a required level is still open. */
    val currentValue: AddressPath?
        get() = if (Level.entries.filter { it <= requiredLevel }.all(::isComplete)) chosenPath() else null

    /** The deepest chosen path even if a required level is still open; what rotation should keep. */
    val selectionPath: AddressPath?
        get() = chosenPath()

    init {
        loadJob = scope.launch {
            load(Level.REGION)
            publish(emit = false)
        }
    }

    /** Chooses [id] at [level] (null clears it); everything below is cleared and the next level is loaded. */
    fun select(level: Level, id: String?) {
        loadJob?.cancel()
        clearBelow(level)
        selected.remove(level)
        if (id != null) options[level].orEmpty().firstOrNull { it.id == id }?.let { selected[level] = it }
        publish(emit = true)
        val next = Level.entries.getOrNull(level.ordinal + 1)
        if (next != null && selected.containsKey(level)) {
            loadJob = scope.launch {
                load(next)
                publish(emit = true)
            }
        }
    }

    /** Puts the pickers into the state of [path] (null = empty) without reporting a user change. */
    suspend fun restore(path: AddressPath?) {
        loadJob?.cancel()
        options.clear(); loaded.clear(); selected.clear(); error = null
        load(Level.REGION)
        val chain = listOf(
            Level.REGION to path?.region?.code,
            Level.DISTRICT to path?.district?.code,
            Level.WARD to path?.ward?.postcode,
            Level.MTAA to path?.mtaa?.id,
            Level.KITONGOJI to path?.kitongoji?.id,
        )
        for ((level, id) in chain) {
            if (id == null) break
            val option = options[level].orEmpty().firstOrNull { it.id == id } ?: break
            selected[level] = option
            Level.entries.getOrNull(level.ordinal + 1)?.let { load(it) }
        }
        lastValue = currentValue
        publish(emit = false)
    }

    private suspend fun load(level: Level) {
        try {
            val found = when (level) {
                Level.REGION -> repository.regions().map { r -> PickerOption(r.code, r.name) { AddressPath(r) } }
                Level.DISTRICT -> repository.districts(parentId(Level.REGION)).map { d -> PickerOption(d.code, d.name) { it!!.copy(district = d) } }
                Level.WARD -> repository.wards(parentId(Level.DISTRICT)).map { w -> PickerOption(w.postcode, w.name) { it!!.copy(ward = w) } }
                Level.MTAA -> repository.mtaas(parentId(Level.WARD)).map { m -> PickerOption(m.id, m.name) { it!!.copy(mtaa = m) } }
                Level.KITONGOJI -> repository.kitongojis(parentId(Level.MTAA)).map { k -> PickerOption(k.id, k.name) { it!!.copy(kitongoji = k) } }
            }
            options[level] = found
            loaded += level
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            options[level] = emptyList()
            error = e.message ?: "Could not load addresses"
        }
    }

    private fun parentId(level: Level): String = selected.getValue(level).id

    private fun clearBelow(level: Level) {
        for (below in Level.entries.filter { it > level }) {
            options.remove(below); loaded.remove(below); selected.remove(below)
        }
        error = null
    }

    private fun publish(emit: Boolean) {
        mutableState.value = snapshot()
        if (emit) {
            val value = currentValue
            if (value != lastValue) {
                lastValue = value
                onValueChange(value)
            }
        }
    }

    private fun chosenPath(): AddressPath? =
        Level.entries.mapNotNull { selected[it] }.fold(null as AddressPath?) { path, option -> option.extend(path) }

    private fun parentLevel(level: Level): Level? = Level.entries.getOrNull(level.ordinal - 1)

    /** True when the parent is chosen (or itself has nothing listed) and there is nothing to pick here. Only mtaa and kitongoji can be empty in the data. */
    private fun noneListed(level: Level): Boolean {
        if (level < Level.MTAA) return false
        val parent = parentLevel(level) ?: return false
        return (selected.containsKey(parent) && level in loaded && options[level].orEmpty().isEmpty()) ||
            (level > Level.MTAA && noneListed(parent))
    }

    private fun isComplete(level: Level): Boolean = selected.containsKey(level) || noneListed(level)

    private fun snapshot(): PickerUiState {
        val levels = Level.entries.map { level ->
            val list = options[level].orEmpty()
            val parent = parentLevel(level)
            PickerLevelState(
                level = level,
                options = list,
                selectedId = selected[level]?.id,
                enabled = list.isNotEmpty() && (parent == null || selected.containsKey(parent)),
                optional = level > requiredLevel,
                noneListed = noneListed(level),
            )
        }
        return PickerUiState(levels, postcode = selected[Level.WARD]?.id, error = error)
    }
}
