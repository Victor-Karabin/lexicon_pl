package com.lexicon.domain.training

import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.interactors.training.StepOutcome

fun StepOutcome.toBoundary(): TrainingResultOutcomeBoundary =
    when (this) {
        StepOutcome.CORRECT -> TrainingResultOutcomeBoundary.CORRECT
        StepOutcome.INCORRECT -> TrainingResultOutcomeBoundary.INCORRECT
        StepOutcome.SKIPPED -> TrainingResultOutcomeBoundary.SKIPPED
    }
