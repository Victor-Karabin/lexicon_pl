package com.lexicon.application.fillword

import com.lexicon.application.training.recordOutcome
import com.lexicon.boundary.SessionStore
import com.lexicon.interactors.fillword.SubmitFillwordRequest
import com.lexicon.interactors.fillword.SubmitFillwordUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.Step
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType

class SubmitFillwordUseCaseImpl(
    private val recordAnswer: RecordAnswerUseCase,
    private val sessions: SessionStore,
) : SubmitFillwordUseCase {
    override suspend fun invoke(request: SubmitFillwordRequest) {
        val session = sessions.find(SessionId(request.sessionId)) ?: return

        session.steps.filterIsInstance<Step.Question>().forEach { step ->
            val found = step.expectedAnswer.uppercase() in request.foundWords
            val outcome = if (found) StepOutcome.CORRECT else StepOutcome.INCORRECT
            if (sessions.recordOutcome(request.sessionId, step.index, outcome)) {
                recordAnswer(
                    RecordedAnswer(
                        sessionId = request.sessionId,
                        trainingType = TrainingType.FILLWORD,
                        stepIndex = step.index,
                        vocabularyItemId = step.wordId.value,
                        expectedAnswer = step.expectedAnswer,
                        submittedAnswer = if (found) step.expectedAnswer else "",
                        outcome = outcome,
                        tipUsed = false,
                    ),
                )
            }
        }
    }
}
