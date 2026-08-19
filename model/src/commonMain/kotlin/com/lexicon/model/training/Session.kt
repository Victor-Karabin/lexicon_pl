package com.lexicon.model.training

import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class SessionId(val value: String)

data class Step(
    val index: Int,
    val wordId: VocabularyId,
    val expectedAnswer: String,
    val outcome: StepOutcome? = null,
    val tipUsed: Boolean = false,
) {
    val isAnswered: Boolean get() = outcome != null
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

    fun answer(
        index: Int,
        outcome: StepOutcome,
        tipUsed: Boolean = false,
    ): Session {
        val step = step(index)
        if (step.isAnswered) {
            throw StepAlreadyAnswered("step $index of session ${id.value} was already answered ${step.outcome}")
        }
        val answered = step.copy(outcome = outcome, tipUsed = tipUsed)
        return copy(steps = steps.toMutableList().also { it[index] = answered }.toImmutableList())
    }

    private fun countOf(outcome: StepOutcome): Int = steps.count { it.outcome == outcome }
}
