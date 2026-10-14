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
import com.lexicon.presentation.common.wordRowActionsWidth
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
        val canDelete = word.isUserCreated
        SwipeToRevealContainer(
            revealWidth = wordRowActionsWidth(canDelete),
            enabled = !selection.isActive,
            collapseSignal = selection.isActive,
            backgroundContent = {
                WordRowActions(
                    onChangePresets = { onChangePresets(word) },
                    onDelete = if (canDelete) ({ onDelete(word) }) else null,
                )
            },
        ) {
            VocabularyWordRow(
                word = word,
                status = statuses.statusOf(word.id),
                onStatusCycled = { onStatusCycled(word.id) },
                onPronounce = { onPronounce(word) },
                onClick = {
                    when {
                        !selection.isActive -> onEdit(word)
                        canDelete -> selection.toggle(word.id)
                    }
                },
                isSelecting = selection.isActive && canDelete,
                isSelected = selection.contains(word.id),
                onLongClick = if (canDelete) ({ selection.toggle(word.id) }) else null,
            )
        }
        if (index < words.lastIndex) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spacingMedium))
        }
    }
}
