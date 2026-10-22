@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.application.training

import com.lexicon.boundary.SessionStore
import com.lexicon.model.training.Session
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.Step
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import kotlinx.collections.immutable.toImmutableList
import kotlin.jvm.JvmName
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
                    Step.Question(index = index, wordId = wordId, expectedAnswer = expected)
                }.toImmutableList(),
            ),
        )
    }
    return id
}

@JvmName("openForWords")
internal suspend fun SessionStore.open(
    training: TrainingType,
    words: List<Word>,
): SessionId = open(training, words.map { it.id to it.text })

suspend fun SessionStore.stepAt(
    sessionId: String,
    stepIndex: Int,
): Step.Question? = find(SessionId(sessionId))?.question(stepIndex)

suspend fun SessionStore.openBoards(
    training: TrainingType,
    boards: List<List<VocabularyId>>,
): SessionId {
    val id = SessionId(Uuid.random().toString())
    val usable = boards.filter { it.isNotEmpty() }
    if (usable.isNotEmpty()) {
        save(
            Session(
                id = id,
                training = training,
                steps = usable.mapIndexed { index, words ->
                    Step.Board(index = index, wordIds = words.toImmutableList())
                }.toImmutableList(),
            ),
        )
    }
    return id
}

suspend fun SessionStore.recordOutcome(
    sessionId: String,
    stepIndex: Int,
    outcome: StepOutcome,
    tipUsed: Boolean = false,
): Boolean {
    var alreadyAnswered = false
    update(SessionId(sessionId)) { session ->
        val step = session.steps.getOrNull(stepIndex)
        when {
            step == null -> session
            step.isAnswered -> session.also { alreadyAnswered = true }
            else -> session.answer(stepIndex, outcome, tipUsed)
        }
    }
    return !alreadyAnswered
}
