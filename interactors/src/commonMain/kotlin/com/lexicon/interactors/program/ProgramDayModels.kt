package com.lexicon.interactors.program

import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class QueuedTraining(
    val training: String,
    val round: Int,
    val isDone: Boolean,
)

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

    val showCardsNext: Boolean get() = !cardsSeen && newWords.isNotEmpty()

    val nextTraining: QueuedTraining? get() = queue.firstOrNull { !it.isDone }
}

data class WordCard(
    val id: VocabularyId,
    val text: String,
    val translation: String,
    val transcription: String,
    val imageUrl: String?,
)
