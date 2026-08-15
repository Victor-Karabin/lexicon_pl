package com.lexicon.presentation.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.graphics.vector.ImageVector
import com.lexicon.presentation.R

object TrainingIds {
    const val DICTATION = "dictation"
    const val DICTATION_PUZZLE = "dictation_puzzle"
    const val TRUE_OR_FALSE = "true_or_false"
    const val WORD_MATCH = "word_match"
    const val PRONUNCIATION_CHECK = "pronunciation_check"
    const val PUZZLE = "puzzle"
    const val IMAGE_TEST = "image_test"
    const val MEMORY_CARDS = "memory_cards"
    const val MIX = "mix"
    const val CROSSWORD = "crossword"
    const val WORD_CARD = "word_card"
}

/**
 * A training as the list shows it.
 *
 * [icon] and [blurb] are here rather than on the screen because they are facts about
 * the training, and the screen should not have to know ten of them by name.
 */
data class TrainingCatalogEntry(
    val id: String,
    val displayName: String,
    val isEnabled: Boolean,
    val icon: ImageVector,
    val blurb: Int,
)

val trainingCatalog =
    listOf(
        TrainingCatalogEntry(
            id = TrainingIds.DICTATION,
            displayName = "Dictation",
            isEnabled = true,
            icon = Icons.Default.Headphones,
            blurb = R.string.training_dictation_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.DICTATION_PUZZLE,
            displayName = "Dictation Puzzle",
            isEnabled = true,
            icon = Icons.Default.Keyboard,
            blurb = R.string.training_dictation_puzzle_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.PUZZLE,
            displayName = "Puzzle",
            isEnabled = true,
            icon = Icons.Default.Extension,
            blurb = R.string.training_puzzle_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.IMAGE_TEST,
            displayName = "Image Test",
            isEnabled = true,
            icon = Icons.Default.Image,
            blurb = R.string.training_image_test_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.WORD_MATCH,
            displayName = "Word Match",
            isEnabled = true,
            icon = Icons.Default.Link,
            blurb = R.string.training_word_match_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.TRUE_OR_FALSE,
            displayName = "True or False",
            isEnabled = true,
            icon = Icons.Default.QuestionMark,
            blurb = R.string.training_true_or_false_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.PRONUNCIATION_CHECK,
            displayName = "Pronunciation Check",
            isEnabled = true,
            icon = Icons.Default.RecordVoiceOver,
            blurb = R.string.training_pronunciation_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.MEMORY_CARDS,
            displayName = "Memory Cards",
            isEnabled = true,
            icon = Icons.Default.Style,
            blurb = R.string.training_memory_cards_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.CROSSWORD,
            displayName = "Crossword",
            isEnabled = true,
            icon = Icons.Default.GridOn,
            blurb = R.string.training_crossword_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.WORD_CARD,
            displayName = "Word Card",
            isEnabled = true,
            icon = Icons.Default.CreditCard,
            blurb = R.string.training_word_card_blurb,
        ),
        TrainingCatalogEntry(
            id = TrainingIds.MIX,
            displayName = "Mix",
            isEnabled = true,
            icon = Icons.Default.AutoAwesome,
            blurb = R.string.training_mix_blurb,
        ),
    )

/**
 * Trainings a program may draw on.
 *
 * Memory Cards picks its pairs at random from whatever list it is given, and
 * Crossword needs eight grid-placeable words and cannot take phrases — either one
 * quietly practises a different set than the program chose, which would make the
 * program's count of what was learned untrue.
 */
val programTrainings =
    trainingCatalog.filter { it.isEnabled && it.id !in setOf(TrainingIds.MEMORY_CARDS, TrainingIds.CROSSWORD) }

fun trainingDisplayName(id: String): String = trainingCatalog.firstOrNull { it.id == id }?.displayName ?: id
