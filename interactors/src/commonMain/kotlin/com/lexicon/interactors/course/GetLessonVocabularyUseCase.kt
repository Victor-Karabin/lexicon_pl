package com.lexicon.interactors.course

import com.lexicon.model.vocabulary.Word
import kotlinx.collections.immutable.ImmutableList

interface GetLessonVocabularyUseCase {
    suspend operator fun invoke(id: LessonId): ImmutableList<Word>
}
