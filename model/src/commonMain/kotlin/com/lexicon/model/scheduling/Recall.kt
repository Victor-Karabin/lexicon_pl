package com.lexicon.model.scheduling

import com.lexicon.model.training.StepOutcome

fun StepOutcome.recallQuality(tipUsed: Boolean): RecallQuality? =
    when (this) {
        StepOutcome.CORRECT -> if (tipUsed) RecallQuality.HESITANT else RecallQuality.PERFECT
        StepOutcome.SKIPPED -> RecallQuality.SKIPPED
        StepOutcome.INCORRECT -> RecallQuality.FORGOTTEN

        StepOutcome.SEEN -> null
    }
