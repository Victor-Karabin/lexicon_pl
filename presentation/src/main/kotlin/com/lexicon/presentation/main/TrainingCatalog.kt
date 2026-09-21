package com.lexicon.presentation.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Ballot
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Style
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.lexicon.model.training.TrainingType
import com.lexicon.presentation.R

object TrainingIds {
    val DICTATION = TrainingType.DICTATION.id
    val DICTATION_PUZZLE = TrainingType.DICTATION_PUZZLE.id
    val TRUE_OR_FALSE = TrainingType.TRUE_OR_FALSE.id
    val WORD_MATCH = TrainingType.WORD_MATCH.id
    val PRONUNCIATION_CHECK = TrainingType.PRONUNCIATION_CHECK.id
    val PRONUNCIATION_SENTENCES = TrainingType.PRONUNCIATION_SENTENCES.id
    val PUZZLE = TrainingType.PUZZLE.id
    val IMAGE_TEST = TrainingType.IMAGE_TEST.id
    val MEMORY_CARDS = TrainingType.MEMORY_CARDS.id
    val MIX = TrainingType.MIX.id
    val CROSSWORD = TrainingType.CROSSWORD.id
    val WORD_CARD = TrainingType.WORD_CARD.id
    val PASSAGE_WRITE = TrainingType.PASSAGE_WRITE.id
    val PASSAGE_BANK = TrainingType.PASSAGE_BANK.id
    val FILLWORD = TrainingType.FILLWORD.id
    val CONJUGATION = TrainingType.CONJUGATION.id
}

data class TrainingCatalogEntry(
    val id: String,
    @StringRes val title: Int,
    val isEnabled: Boolean,
    val icon: ImageVector,
    @StringRes val blurb: Int,
)

val trainingCatalog =
    listOf(
        TrainingCatalogEntry(
            id = TrainingIds.DICTATION,
            title = R.string.dictation_title,
            isEnabled = true,
            icon = Icons.Default.Headphones,
            blurb = R.string.training_dictation_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.DICTATION_PUZZLE,
            title = R.string.dictation_puzzle_title,
            isEnabled = true,
            icon = Icons.Default.Keyboard,
            blurb = R.string.training_dictation_puzzle_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.PUZZLE,
            title = R.string.puzzle_title,
            isEnabled = true,
            icon = Icons.Default.Extension,
            blurb = R.string.training_puzzle_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.IMAGE_TEST,
            title = R.string.image_test_title,
            isEnabled = true,
            icon = Icons.Default.Image,
            blurb = R.string.training_image_test_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.WORD_MATCH,
            title = R.string.word_match_title,
            isEnabled = true,
            icon = Icons.Default.Link,
            blurb = R.string.training_word_match_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.TRUE_OR_FALSE,
            title = R.string.true_or_false_title,
            isEnabled = true,
            icon = Icons.Default.QuestionMark,
            blurb = R.string.training_true_or_false_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.PRONUNCIATION_CHECK,
            title = R.string.pronunciation_title,
            isEnabled = true,
            icon = Icons.Default.RecordVoiceOver,
            blurb = R.string.training_pronunciation_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.PRONUNCIATION_SENTENCES,
            title = R.string.pronunciation_sentences_title,
            isEnabled = true,
            icon = Icons.Default.Mic,
            blurb = R.string.training_pronunciation_sentences_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.MEMORY_CARDS,
            title = R.string.memory_cards_title,
            isEnabled = true,
            icon = Icons.Default.Style,
            blurb = R.string.training_memory_cards_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.CROSSWORD,
            title = R.string.crossword_title,
            isEnabled = true,
            icon = Icons.Default.GridOn,
            blurb = R.string.training_crossword_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.WORD_CARD,
            title = R.string.word_card_title,
            isEnabled = true,
            icon = Icons.Default.CreditCard,
            blurb = R.string.training_word_card_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.PASSAGE_WRITE,
            title = R.string.passage_write_title,
            isEnabled = true,
            icon = Icons.AutoMirrored.Filled.Notes,
            blurb = R.string.training_passage_write_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.PASSAGE_BANK,
            title = R.string.passage_bank_title,
            isEnabled = true,
            icon = Icons.Default.Ballot,
            blurb = R.string.training_passage_bank_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.FILLWORD,
            title = R.string.fillword_title,
            isEnabled = true,
            icon = Icons.Default.GridOn,
            blurb = R.string.training_fillword_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.MIX,
            title = R.string.mix_title,
            isEnabled = true,
            icon = Icons.Default.AutoAwesome,
            blurb = R.string.training_mix_blurb,
        ),
    )

val courseTrainings = trainingCatalog.filter { it.isEnabled }

@Composable
fun List<TrainingCatalogEntry>.sortedByTitle(): List<TrainingCatalogEntry> {
    val resources = LocalContext.current.resources
    val configuration = LocalConfiguration.current
    return remember(this, configuration) { sortedBy { resources.getString(it.title).lowercase() } }
}

@Composable
fun trainingDisplayName(id: String): String = trainingCatalog.firstOrNull { it.id == id }?.let { stringResource(it.title) } ?: id
