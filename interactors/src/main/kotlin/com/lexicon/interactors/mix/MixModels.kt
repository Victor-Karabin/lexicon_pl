package com.lexicon.interactors.mix

import com.lexicon.interactors.dictation.DictationStepResponse
import com.lexicon.interactors.dictationpuzzle.DictationPuzzleStepResponse
import com.lexicon.interactors.imagetest.ImageTestStepResponse
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.puzzle.PuzzleStepResponse
import com.lexicon.interactors.trueorfalse.TrueOrFalseStepResponse

/**
 * Training types a Mix session can generate a step from.
 *
 * Excluded, and why:
 * - Word Match and Memory Cards are whole-board exercises rather than single-item steps; one Mix
 *   step can't meaningfully hold a grid of pairs or cards.
 * - Crossword is a single-screen training with no step workflow at all.
 */
enum class MixTrainingType {
    DICTATION,
    DICTATION_PUZZLE,
    PUZZLE,
    IMAGE_TEST,
    TRUE_OR_FALSE,
    PRONUNCIATION_CHECK,
}

/**
 * One Mix step, carrying the originating training's own step payload so the step behaves exactly
 * like it does standalone (Mix spec §9).
 */
sealed interface MixStep {
    val stepIndex: Int
    val trainingType: MixTrainingType

    data class Dictation(override val stepIndex: Int, val step: DictationStepResponse) : MixStep {
        override val trainingType = MixTrainingType.DICTATION
    }

    data class DictationPuzzle(override val stepIndex: Int, val step: DictationPuzzleStepResponse) : MixStep {
        override val trainingType = MixTrainingType.DICTATION_PUZZLE
    }

    data class Puzzle(override val stepIndex: Int, val step: PuzzleStepResponse) : MixStep {
        override val trainingType = MixTrainingType.PUZZLE
    }

    data class ImageTest(override val stepIndex: Int, val step: ImageTestStepResponse) : MixStep {
        override val trainingType = MixTrainingType.IMAGE_TEST
    }

    /** Timerless: the countdown belongs to a standalone True or False session, not to one Mix step. */
    data class TrueOrFalse(override val stepIndex: Int, val step: TrueOrFalseStepResponse) : MixStep {
        override val trainingType = MixTrainingType.TRUE_OR_FALSE
    }

    data class Pronunciation(override val stepIndex: Int, val step: PronunciationStepResponse) : MixStep {
        override val trainingType = MixTrainingType.PRONUNCIATION_CHECK
    }
}

data class MixSessionResponse(
    val sessionId: String,
    val steps: List<MixStep>,
)
