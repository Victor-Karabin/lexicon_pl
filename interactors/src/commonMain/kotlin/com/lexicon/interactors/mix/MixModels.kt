package com.lexicon.interactors.mix

import com.lexicon.interactors.dictation.DictationStepResponse
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleStepResponse
import com.lexicon.interactors.imagetest.ImageTestStepResponse
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.puzzle.PuzzleStepResponse
import com.lexicon.interactors.trueorfalse.TrueOrFalseStepResponse

enum class MixTrainingType {
    DICTATION,
    DICTATION_PUZZLE,
    PUZZLE,
    IMAGE_TEST,
    TRUE_OR_FALSE,
    PRONUNCIATION_CHECK,
}

sealed interface MixStep {
    val stepIndex: Int
    val trainingType: MixTrainingType

    val vocabularyItemId: Long

    data class Dictation(override val stepIndex: Int, val step: DictationStepResponse) : MixStep {
        override val trainingType = MixTrainingType.DICTATION
        override val vocabularyItemId = step.vocabularyItemId
    }

    data class DictationPuzzle(override val stepIndex: Int, val step: DictationPuzzleStepResponse) : MixStep {
        override val trainingType = MixTrainingType.DICTATION_PUZZLE
        override val vocabularyItemId = step.vocabularyItemId
    }

    data class Puzzle(override val stepIndex: Int, val step: PuzzleStepResponse) : MixStep {
        override val trainingType = MixTrainingType.PUZZLE
        override val vocabularyItemId = step.vocabularyItemId
    }

    data class ImageTest(override val stepIndex: Int, val step: ImageTestStepResponse) : MixStep {
        override val trainingType = MixTrainingType.IMAGE_TEST
        override val vocabularyItemId = step.vocabularyItemId
    }

    data class TrueOrFalse(override val stepIndex: Int, val step: TrueOrFalseStepResponse) : MixStep {
        override val trainingType = MixTrainingType.TRUE_OR_FALSE
        override val vocabularyItemId = step.vocabularyItemId
    }

    data class Pronunciation(override val stepIndex: Int, val step: PronunciationStepResponse) : MixStep {
        override val trainingType = MixTrainingType.PRONUNCIATION_CHECK
        override val vocabularyItemId = step.vocabularyItemId
    }
}

data class MixSessionResponse(
    val sessionId: String,
    val steps: List<MixStep>,
)
