package com.lexicon.presentation.presets

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Modifier
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.presentation.common.SwipeToRevealContainer
import com.lexicon.presentation.common.WordRowActions
import com.lexicon.presentation.common.WordRowActionsWidth
import com.lexicon.presentation.theme.Dimens

/**
 * The word list as it looks everywhere a word can be acted on — the Vocabulary tab's
 * results and a preset's own contents. Both need the same row, the same swipe
 * actions and the same dividers, so they share this rather than each keeping a copy.
 */
fun LazyListScope.wordRows(
    words: List<PresetWord>,
    onFavouriteToggled: (VocabularyId, Boolean) -> Unit,
    onPronounce: (PresetWord) -> Unit,
    onChangePresets: (PresetWord) -> Unit,
    onDelete: (PresetWord) -> Unit,
    onEdit: (PresetWord) -> Unit,
) {
    itemsIndexed(words, key = { _, word -> word.id.value }) { index, word ->
        SwipeToRevealContainer(
            revealWidth = WordRowActionsWidth,
            backgroundContent = {
                WordRowActions(
                    onChangePresets = { onChangePresets(word) },
                    onDelete = { onDelete(word) },
                )
            },
        ) {
            VocabularyWordRow(
                word = word,
                onFavouriteToggled = { onFavouriteToggled(word.id, !word.isFavourite) },
                onPronounce = { onPronounce(word) },
                onClick = { onEdit(word) },
            )
        }
        if (index < words.lastIndex) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spacingMedium))
        }
    }
}
