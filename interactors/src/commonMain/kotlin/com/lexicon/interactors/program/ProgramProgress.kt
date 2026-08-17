package com.lexicon.interactors.program

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ProgramProgress(
    val programId: ProgramId,
    val metrics: ImmutableList<ProgressMetric> = persistentListOf(),
) {
    /** The weighted whole, 0.0 to 1.0. Weights sum to 100, checked when the asset is built. */
    val overall: Double
        get() {
            val totalWeight = metrics.sumOf { it.weight }
            if (totalWeight == 0) return 0.0
            return metrics.sumOf { it.fraction * it.weight } / totalWeight
        }
}

/** One metric's contribution, kept apart so the UI can show the breakdown. */
data class ProgressMetric(
    val type: ProgressMetricType,
    val current: Int,
    val target: Int,
    val weight: Int,
    /**
     * False when there is nothing to measure yet — no answers today, say. Distinct
     * from a measured nought, which is a real and much worse figure.
     */
    val isMeasured: Boolean = true,
) {
    val fraction: Double get() = if (target <= 0) 1.0 else (current.toDouble() / target).coerceIn(0.0, 1.0)
}

enum class ProgressMetricType { VOCABULARY, MILESTONES, CONSISTENCY, STUDY_TIME, ACCURACY }
