package com.lexicon.data.local

interface AttemptRecord {
    val attempted: Int
    val correct: Int
    val incorrect: Int
    val streak: Int
}

data class AttemptCounts(
    override val attempted: Int = 0,
    override val correct: Int = 0,
    override val incorrect: Int = 0,
    override val streak: Int = 0,
) : AttemptRecord {
    fun after(isCorrect: Boolean): AttemptCounts =
        AttemptCounts(
            attempted = attempted + 1,
            correct = correct + if (isCorrect) 1 else 0,
            incorrect = incorrect + if (isCorrect) 0 else 1,
            streak = if (isCorrect) streak + 1 else 0,
        )

    companion object {
        fun of(row: AttemptRecord?): AttemptCounts =
            AttemptCounts(
                attempted = row?.attempted ?: 0,
                correct = row?.correct ?: 0,
                incorrect = row?.incorrect ?: 0,
                streak = row?.streak ?: 0,
            )
    }
}
