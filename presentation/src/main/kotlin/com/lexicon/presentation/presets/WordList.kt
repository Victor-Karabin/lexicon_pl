package com.lexicon.presentation.presets

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Modifier
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import com.lexicon.model.vocabulary.statusOf
import com.lexicon.presentation.common.SwipeToRevealContainer
import com.lexicon.presentation.common.WordRowActions
import com.lexicon.presentation.common.WordRowActionsWidth
import com.lexicon.presentation.theme.Dimens

fun LazyListScope.wordRows(
    words: List<Word>,
    statuses: Map<VocabularyId, WordStatus>,
    onStatusCycled: (VocabularyId) -> Unit,
    onPronounce: (Word) -> Unit,
    onChangePresets: (Word) -> Unit,
    onDelete: (Word) -> Unit,
    onEdit: (Word) -> Unit,
    selection: WordSelection,
) {
    itemsIndexed(words, key = { _, word -> word.id.value }) { index, word ->
        SwipeToRevealContainer(
            revealWidth = WordRowActionsWidth,
            enabled = !selection.isActive,
            collapseSignal = selection.isActive,
            backgroundContent = {
                WordRowActions(
                    onChangePresets = { onChangePresets(word) },
                    onDelete = { onDelete(word) },
                )
            },
        ) {
            VocabularyWordRow(
                word = word,
                status = statuses.statusOf(word.id),
                onStatusCycled = { onStatusCycled(word.id) },
                onPronounce = { onPronounce(word) },
                onClick = { if (selection.isActive) selection.toggle(word.id) else onEdit(word) },
                isSelecting = selection.isActive,
                isSelected = selection.contains(word.id),
                onLongClick = { selection.toggle(word.id) },
            )
        }
        if (index < words.lastIndex) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spacingMedium))
        }
    }
}
