package com.lexicon.application.training

import com.lexicon.boundary.SessionStore
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType

internal data class TypedAnswer(
    val outcome: StepOutcome,
    val expectedText: String,
)

internal suspend fun SessionStore.submitTyped(
    training: TrainingType,
    sessionId: String,
    stepIndex: Int,
    fallbackExpectedText: String,
    fallbackVocabularyItemId: Long,
    submittedText: String,
    skipped: Boolean,
    tipUsed: Boolean,
    recordAnswer: RecordAnswerUseCase,
    matches: (expected: String, submitted: String) -> Boolean,
): TypedAnswer {
    val step = stepAt(sessionId, stepIndex)
    val expectedText = step?.expectedAnswer ?: fallbackExpectedText
    val outcome = when {
        skipped -> StepOutcome.SKIPPED
        matches(expectedText, submittedText) -> StepOutcome.CORRECT
        else -> StepOutcome.INCORRECT
    }

    if (recordOutcome(sessionId, stepIndex, outcome, tipUsed)) {
        recordAnswer(
            RecordedAnswer(
                sessionId = sessionId,
                trainingType = training,
                stepIndex = stepIndex,
                vocabularyItemId = step?.wordId?.value ?: fallbackVocabularyItemId,
                expectedAnswer = expectedText,
                submittedAnswer = submittedText,
                outcome = outcome,
                tipUsed = tipUsed,
            ),
        )
    }

    return TypedAnswer(outcome = outcome, expectedText = expectedText)
}
