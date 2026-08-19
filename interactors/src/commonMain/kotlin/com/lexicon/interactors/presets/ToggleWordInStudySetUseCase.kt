package com.lexicon.interactors.presets

interface ToggleWordInStudySetUseCase {
    suspend operator fun invoke(
        id: VocabularyId,
        isInStudySet: Boolean,
    )
}
