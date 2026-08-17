package com.lexicon.interactors.course

import com.lexicon.interactors.presets.PresetWord
import kotlinx.collections.immutable.ImmutableList

interface GetLessonVocabularyUseCase {
    suspend operator fun invoke(id: LessonId): ImmutableList<PresetWord>
}
