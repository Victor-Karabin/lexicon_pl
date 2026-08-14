package com.lexicon.presentation.presets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.lexicon.interactors.presets.VocabularyId

/**
 * Which words are ticked, and therefore whether the list is in selection mode at all.
 *
 * Held here rather than in a ViewModel because both word lists — the Vocabulary tab
 * and a preset's contents — need the same behaviour, and neither needs the selection
 * to outlive the screen. It does survive rotation, which is why it is saveable: a set
 * of ticks is tedious to rebuild by hand.
 */
@Stable
class WordSelection(
    initial: Set<VocabularyId> = emptySet(),
) {
    var selected: Set<VocabularyId> by mutableStateOf(initial)
        private set

    /** Selection mode is simply having something selected; there is no separate flag. */
    val isActive: Boolean get() = selected.isNotEmpty()

    val count: Int get() = selected.size

    fun contains(id: VocabularyId): Boolean = id in selected

    fun toggle(id: VocabularyId) {
        selected = if (id in selected) selected - id else selected + id
    }

    fun clear() {
        selected = emptySet()
    }

    companion object {
        val Saver: Saver<WordSelection, List<Long>> = Saver(
            save = { selection -> selection.selected.map { it.value } },
            restore = { ids -> WordSelection(ids.mapTo(mutableSetOf(), ::VocabularyId)) },
        )
    }
}

@Composable
fun rememberWordSelection(): WordSelection = rememberSaveable(saver = WordSelection.Saver) { WordSelection() }
