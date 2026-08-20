package com.lexicon.presentation.presets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.lexicon.model.vocabulary.VocabularyId

@Stable
class WordSelection(
    initial: Set<VocabularyId> = emptySet(),
) {
    var selected: Set<VocabularyId> by mutableStateOf(initial)
        private set

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
