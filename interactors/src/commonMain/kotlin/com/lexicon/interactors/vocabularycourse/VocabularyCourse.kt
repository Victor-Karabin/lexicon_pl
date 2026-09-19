package com.lexicon.interactors.vocabularycourse

import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CourseSettings(
    val newWordsADay: Int = DEFAULT_NEW_WORDS_A_DAY,
    val reviewsADay: Int = DEFAULT_REVIEWS_A_DAY,
    val queue: List<String> = defaultCourseQueue,
) {
    companion object {
        const val DEFAULT_NEW_WORDS_A_DAY = 10
        const val DEFAULT_REVIEWS_A_DAY = 15
        const val MAX_WORDS_A_DAY = 50
    }
}

data class VocabularyCourse(
    val settings: CourseSettings = CourseSettings(),
    val position: Int = 0,
    val round: Int = 0,
    val cardsSeen: Boolean = false,
    val newWords: ImmutableList<VocabularyId> = persistentListOf(),
) {
    val trainings: List<String> get() = settings.queue

    val totalTrainings: Int get() = trainings.size

    val completedInRound: Int get() = position.coerceIn(0, totalTrainings)

    val roundFraction: Float get() = if (totalTrainings == 0) 0f else completedInRound.toFloat() / totalTrainings

    val nextTraining: TrainingType? get() = trainings.getOrNull(position)?.let(TrainingType::ofId)

    val showCardsNext: Boolean get() = position == 0 && !cardsSeen && newWords.isNotEmpty()
}

val defaultCourseQueue: List<String> =
    listOf(
        TrainingType.WORD_CARD,
        TrainingType.IMAGE_TEST,
        TrainingType.WORD_MATCH,
        TrainingType.TRUE_OR_FALSE,
        TrainingType.PUZZLE,
        TrainingType.DICTATION_PUZZLE,
        TrainingType.DICTATION,
        TrainingType.PASSAGE_BANK,
        TrainingType.PASSAGE_WRITE,
        TrainingType.PRONUNCIATION_CHECK,
        TrainingType.PRONUNCIATION_SENTENCES,
        TrainingType.FILLWORD,
        TrainingType.MEMORY_CARDS,
        TrainingType.CROSSWORD,
    ).map { it.id }

data class WordCard(
    val id: VocabularyId,
    val text: String,
    val translation: String,
    val transcription: String,
    val imageUrl: String?,
    val example: String = "",
)

data class CourseLaunch(
    val training: TrainingType,
    val wordIds: ImmutableList<VocabularyId>,
)
