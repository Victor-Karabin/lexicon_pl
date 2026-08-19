package com.lexicon.model.training

import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

data class SessionId(val value: String)

/**
 * Most trainings ask about one word and have one right answer. Memory Cards and Word
 * Match instead put several words on the board and ask for them to be paired, so there
 * is no single expected answer to hold. Both are steps; only one of them can promise an
 * expected answer, and the type says which.
 */
sealed interface Step {
    val index: Int
    val outcome: StepOutcome?
    val tipUsed: Boolean
    val wordIds: ImmutableList<VocabularyId>

    val isAnswered: Boolean get() = outcome != null

    data class Question(
        override val index: Int,
        val wordId: VocabularyId,
        val expectedAnswer: String,
        override val outcome: StepOutcome? = null,
        override val tipUsed: Boolean = false,
    ) : Step {
        override val wordIds: ImmutableList<VocabularyId> get() = persistentListOf(wordId)
    }

    data class Board(
        override val index: Int,
        override val wordIds: ImmutableList<VocabularyId>,
        override val outcome: StepOutcome? = null,
        override val tipUsed: Boolean = false,
    ) : Step
}

private fun Step.answered(
    outcome: StepOutcome,
    tipUsed: Boolean,
): Step =
    when (this) {
        is Step.Question -> copy(outcome = outcome, tipUsed = tipUsed)
        is Step.Board -> copy(outcome = outcome, tipUsed = tipUsed)
    }

class StepAlreadyAnswered(message: String) : IllegalStateException(message)

class NoSuchStep(message: String) : IllegalArgumentException(message)

data class Session(
    val id: SessionId,
    val training: TrainingType,
    val steps: ImmutableList<Step>,
) {
    init {
        require(steps.isNotEmpty()) { "a session must have at least one step" }
        require(steps.mapIndexed { position, step -> step.index == position }.all { it }) {
            "a session's steps must be numbered from zero, in order"
        }
    }

    val isComplete: Boolean get() = steps.all { it.isAnswered }

    val currentStep: Step? get() = steps.firstOrNull { !it.isAnswered }

    val correctCount: Int get() = countOf(StepOutcome.CORRECT)

    val incorrectCount: Int get() = countOf(StepOutcome.INCORRECT)

    val skippedCount: Int get() = countOf(StepOutcome.SKIPPED)

    val tipsUsedCount: Int get() = steps.count { it.tipUsed }

    fun step(index: Int): Step = steps.getOrNull(index) ?: throw NoSuchStep("session ${id.value} has no step $index")

    fun question(index: Int): Step.Question? = steps.getOrNull(index) as? Step.Question

    fun answer(
        index: Int,
        outcome: StepOutcome,
        tipUsed: Boolean = false,
    ): Session {
        val step = step(index)
        if (step.isAnswered) {
            throw StepAlreadyAnswered("step $index of session ${id.value} was already answered ${step.outcome}")
        }
        return copy(steps = steps.toMutableList().also { it[index] = step.answered(outcome, tipUsed) }.toImmutableList())
    }

    private fun countOf(outcome: StepOutcome): Int = steps.count { it.outcome == outcome }
}
