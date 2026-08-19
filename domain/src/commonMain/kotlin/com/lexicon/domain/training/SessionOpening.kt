@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.domain.training

import com.lexicon.boundary.SessionStore
import com.lexicon.model.training.Session
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.Step
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.toImmutableList
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Opens a session over the words a training drew, so the expected answers live with
 * the session rather than travelling back in from whoever submits. A draw of nothing
 * is not a session: the aggregate rejects it, and the training gate keeps it from
 * happening.
 */
suspend fun SessionStore.open(
    training: TrainingType,
    answers: List<Pair<VocabularyId, String>>,
): SessionId {
    val id = SessionId(Uuid.random().toString())
    if (answers.isNotEmpty()) {
        save(
            Session(
                id = id,
                training = training,
                steps = answers.mapIndexed { index, (wordId, expected) ->
                    Step(index = index, wordId = wordId, expectedAnswer = expected)
                }.toImmutableList(),
            ),
        )
    }
    return id
}

suspend fun SessionStore.stepAt(
    sessionId: String,
    stepIndex: Int,
): Step? = find(SessionId(sessionId))?.steps?.getOrNull(stepIndex)

suspend fun SessionStore.recordOutcome(
    sessionId: String,
    stepIndex: Int,
    outcome: StepOutcome,
    tipUsed: Boolean = false,
) {
    val id = SessionId(sessionId)
    val session = find(id) ?: return
    val step = session.steps.getOrNull(stepIndex) ?: return
    if (step.isAnswered) return
    save(session.answer(stepIndex, outcome, tipUsed))
}
