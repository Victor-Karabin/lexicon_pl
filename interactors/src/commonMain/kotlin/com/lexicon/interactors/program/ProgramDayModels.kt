package com.lexicon.interactors.program

import com.lexicon.interactors.presets.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** One turn at one training, and whether it has been taken. */
data class QueuedTraining(
    val training: String,
    /** Which pass this is: 0 the first time today, 1 the second. */
    val round: Int,
    val isDone: Boolean,
)

/**
 * A day's work: the new words to meet, then the trainings to work through.
 *
 * The word list and the queue are fixed when the day is first opened, so the work
 * does not shift between looking at it and starting it. What has been *done*,
 * though, is read from the training history rather than ticked off here — the work
 * counts because it was answered, wherever the learner started it from.
 */
data class ProgramDay(
    val programId: ProgramId,
    val epochDay: Long,
    val newWords: ImmutableList<VocabularyId> = persistentListOf(),
    val cardsSeen: Boolean = false,
    val queue: ImmutableList<QueuedTraining> = persistentListOf(),
) {
    val totalTrainings: Int get() = queue.size

    val completedTrainings: Int get() = queue.count { it.isDone }

    val isComplete: Boolean get() = queue.isNotEmpty() && queue.all { it.isDone }

    /** New words are met before anything is drilled, and only once. */
    val showCardsNext: Boolean get() = !cardsSeen && newWords.isNotEmpty()

    val nextTraining: QueuedTraining? get() = queue.firstOrNull { !it.isDone }
}

/** A word as the card deck shows it. */
data class WordCard(
    val id: VocabularyId,
    val text: String,
    val translation: String,
    val transcription: String,
    val imageUrl: String?,
)
